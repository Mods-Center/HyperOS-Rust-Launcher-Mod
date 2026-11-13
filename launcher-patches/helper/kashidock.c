// SPDX-License-Identifier: AGPL-3.0-or-later
// libkashidock.so - runtime values for the patched HyperOS 4 launcher (com.miui.home 8.01.02.9239).
// Loaded as a DT_NEEDED of libapp_launcher.so. Reads Settings.System kashi_* keys through the
// launcher's own hyper_os C API and publishes them into the data page the libapp.so islands read.
#include <android/log.h>
#include <dlfcn.h>
#include <link.h>
#include <pthread.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

#define TAG "KashiDock"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

// Filled in by the patcher (kept in .data so the compiler cannot fold them).
struct kashi_cfg { char tag[8]; uint64_t libapp_off, data_vaddr, runtime_off; };
__attribute__((used)) volatile struct kashi_cfg kashi_cfg = {{'K','A','S','H','I','C','F','G'}, 0, 0x1c4c000, 0xf68130};

__attribute__((weak)) extern void *content_resolver_new(void *runtime_ref);
__attribute__((weak)) extern void content_resolver_delete(void *resolver);
__attribute__((weak)) extern uint64_t settings_system_get_i32(void *resolver, const char *name, size_t len);
__attribute__((weak)) extern uint64_t settings_secure_get_i32(void *resolver, const char *name, size_t len);
// abi_stable RString in an Option-like result: tag 0 = value; the vtable's 4th entry is the destructor (&mut RVec).
struct kashi_rstr { uint8_t tag; uint8_t pad[7]; const char *ptr; size_t len; size_t cap; void *const *vt; };
__attribute__((weak)) extern struct kashi_rstr settings_system_get_string(void *resolver, const char *name, size_t len);
extern int __system_property_get(const char *name, char *value);

static const char kMagic[16] = "KASHIDOCK2";
enum { F_ENABLE, F_STYLE, F_RADIUS, F_PADDING, F_HEIGHT, F_QSB, F_WRESIZE, F_PAID, F_STYLES, F_RBLUR, F_HIDECLEAR, F_ICONFX, F_HIDEAPP, F_IOFF, F_DOFF, F_FULLANIM, F_ICONBIG, F_WSRC, F_MINUS, F_COUNT };
enum { MAX_FIELDS = 32 };
static const char *const kKeys[F_COUNT] = {"kashi_dock_enable", "kashi_dock_style", "kashi_dock_radius",
    "kashi_dock_padding", "kashi_dock_height", "kashi_home_qsb", "kashi_widget_resize", "kashi_paid_widgets",
    "kashi_grid_gap", "kashi_recents_blur", "kashi_recents_hide_clear",
    "kashi_icon_effect", "kashi_hide_any_app", "kashi_indicator_offset", "kashi_dock_icon_offset", "kashi_full_anim",
    "kashi_icon_bigger", "kashi_widget_source", "kashi_minus_screen"};

// Soft glass (HyperChanger HGLS order): blur, then 14 SoftGlass adjustments. Per record (home, dock).
enum { GLASS_PARAMS = 15, GLASS_KEYS = 2 * (GLASS_PARAMS + 1) };
static const char *const kGlassNames[GLASS_PARAMS] = {"blur", "luminance", "saturation", "brightness", "alpha",
    "refraction", "thickness", "bg_saturation", "bg_brightness", "burn", "edge", "edge_feather",
    "reflection_offset", "light", "darker"};
static const int kGlassFieldParam[GLASS_PARAMS] = {10, 6, 12, 13, 14, 5, 11, 7, 8, 1, 2, 3, 0, 4, 9};
enum { GLASS_OFFSET = 0x200 };
static char glass_keys[GLASS_KEYS][48];
static int32_t glass_cur[GLASS_KEYS], glass_def[GLASS_KEYS];
static void glass_init(void) {
    for (int r = 0; r < 2; ++r) {
        const char *rec = r ? "dock" : "home";
        int base = r * (GLASS_PARAMS + 1);
        snprintf(glass_keys[base], sizeof glass_keys[0], "kashi_glass_%s_on", rec);
        glass_def[base] = 0;
        for (int p = 0; p < GLASS_PARAMS; ++p) {
            snprintf(glass_keys[base + 1 + p], sizeof glass_keys[0], "kashi_glass_%s_%s", rec, kGlassNames[p]);
            glass_def[base + 1 + p] = p == 0 ? 100 : 0;
        }
    }
    memcpy(glass_cur, glass_def, sizeof glass_cur);
}
struct page {            // must match patch_launcher.py
    char magic[16];
    uint64_t enable, style;
    uint64_t radius_on; double radius;
    uint64_t padding_on; double padding;
    uint64_t height_on; double height;
    uint64_t qsb;
    uint64_t widget_resize, paid_unlock, search_styles;
    uint64_t hide_clear;             // 1 = no clear-all button in recents (slot was grid_cols)
    double rblur;                    // recents blur strength factor, 1.0 = stock (slot was grid_rows)
    uint64_t icon_fx;                // 1 = HyperOS icon highlight (bloom stroke) on every icon (slot was folder_cols)
    uint64_t hide_app;               // 1 = every app can be hidden on the launcher's "hide apps" page (slot was fsp_on)
    double ioff;                     // extra lift of the page indicator / search capsule above the dock, dp (slot was fsp)
    uint64_t doff_on; double doff;   // dock icon offset, dp, positive = up
    uint64_t full_anim;              // high-end device behaviour
    double icon_factor;              // home icon size multiplier, 1.0 = stock (key kashi_icon_bigger = extra percent)
    uint64_t widget_source;          // 0 auto (ROM), 1 MIUI / CN App Vault, 2 Global App Vault
    int32_t defaults[MAX_FIELDS];   // build-time defaults, used when a key is not set
    double lift_px;                  // 0x140: capsule / dots lift in physical px (grid gives up this much height)
    uint64_t minus;                  // 0x148: -1 screen, 0 stock, 1 App Vault, 2 Google Discover
    uint64_t no_prestart;            // 0x150: 1 = runtime has no pre-startup animation (HyperOS 3 + bridge)
    double os3_dim;                  // 0x158: patcher-owned (HyperOS 3 dimming factor), never written here
    uint64_t tap_mark;               // 0x160: patcher-owned (HyperOS 3 recents tap marker), never written here
    uint64_t slideup;                // 0x168: 1 = swipe-up opens the picked search app (search app picked and the
                                     //        launcher's own swipe-up choice is "Search" or not set)
};

static int clampi(int v, int lo, int hi) { return v < lo ? lo : v > hi ? hi : v; }
static int g_dpi = 0;              // display density (forced value first), 0 = unknown
static int g_slide_search = 1;     // launcher_slideup_gesture is global_search (or unset)

static struct page *find_page(void) {
    FILE *maps = fopen("/proc/self/maps", "re");
    if (!maps) return NULL;
    char line[768];
    struct page *found = NULL;
    while (!found && fgets(line, sizeof line, maps)) {
        unsigned long long s, e, off; char perms[8]; int n = 0;
        if (sscanf(line, "%llx-%llx %7s %llx %*s %*s %n", &s, &e, perms, &off, &n) < 4 || !n) continue;
        if (perms[0] != 'r') continue;
        size_t len = strlen(line);
        while (len && (line[len - 1] == '\n' || line[len - 1] == ' ')) line[--len] = 0;
        const char *path = line + n;
        uint64_t want;
        size_t pl = strlen(path);
        if (pl >= 10 && !strcmp(path + pl - 10, "/libapp.so")) want = kashi_cfg.data_vaddr;
        else if (pl >= 4 && !strcmp(path + pl - 4, ".apk")) want = kashi_cfg.libapp_off + kashi_cfg.data_vaddr;
        else continue;
        if (want < off || want - off >= e - s) continue;
        struct page *p = (struct page *)(uintptr_t)(s + (want - off));
        if (!memcmp(p->magic, kMagic, sizeof kMagic)) found = p;
    }
    fclose(maps);
    return found;
}

static void publish(struct page *p, const int32_t v[F_COUNT]) {
    struct page n = *p;
    const int enable = v[F_ENABLE] != 0, style = clampi(v[F_STYLE], 0, 2);
    n.enable = enable; n.style = style;
    n.radius = clampi(v[F_RADIUS], 0, 60); n.radius_on = enable && style;
    n.padding = clampi(v[F_PADDING], 0, 150); n.padding_on = enable && style;
    n.height = clampi(v[F_HEIGHT], -40, 150); n.height_on = v[F_HEIGHT] != 0;
    n.qsb = (uint64_t)clampi(v[F_QSB], 0, 2);   // 0 stock, 1 picked app (trampoline), 2 Google native
    // paid widgets + search bar styles are always on (no switches any more; old keys may still hold 0)
    n.widget_resize = v[F_WRESIZE] != 0; n.paid_unlock = 1; n.search_styles = 1;
    n.hide_clear = v[F_HIDECLEAR] != 0;
    n.rblur = clampi(v[F_RBLUR], 0, 300) / 100.0;
    n.icon_fx = v[F_ICONFX] != 0;
    n.hide_app = v[F_HIDEAPP] != 0;
    n.ioff = clampi(v[F_IOFF], -40, 80);
    n.doff = clampi(v[F_DOFF], -60, 30); n.doff_on = v[F_DOFF] != 0;
    n.full_anim = v[F_FULLANIM] != 0;
    n.icon_factor = 1.0 + clampi(v[F_ICONBIG], 0, 15) / 100.0;
    n.widget_source = (uint64_t)clampi(v[F_WSRC], 0, 2);
    n.minus = (uint64_t)clampi(v[F_MINUS], 0, 2);
    n.slideup = n.qsb != 0 && g_slide_search;
    // HyperOS 3 (Rust runtime 1.x via the bridge) has no pre-startup animation: a recents card tapped while the
    // app->recents transition is still live gets merged into it and opens without animation. The launcher then
    // finishes that transition to recents first (island at disallowActiveAppWindowFinish).
    n.no_prestart = dlsym(RTLD_DEFAULT, "WindowManager_start_pre_startup_animation") == NULL;
    {   // the grid code works in physical pixels, the keys are dp
        int dpi = g_dpi;
        if (dpi <= 0) { char b[92] = {0}; if (__system_property_get("ro.sf.lcd_density", b) > 0) dpi = atoi(b); }
        if (dpi < 120 || dpi > 960) dpi = 440;
        // grid gap (key index 8, was search styles): the grid gives up this height too, the capsule stays
        n.lift_px = (n.ioff + (n.height_on ? n.height : 0.0) + clampi(v[F_STYLES], 0, 80)) * dpi / 160.0;
    }
    if (!memcmp(&n, p, sizeof n)) return;
    const long ps = sysconf(_SC_PAGESIZE);
    void *base = (void *)((uintptr_t)p & ~(uintptr_t)(ps - 1));
    // Data-only page: never writable and executable at the same time.
    if (mprotect(base, ps, PROT_READ | PROT_WRITE) != 0) { LOGI("mprotect failed"); return; }
    // Each island reads one flag and one value; aligned 64-bit stores are atomic.
    volatile uint64_t *d = (volatile uint64_t *)p, *s = (uint64_t *)&n;
    for (size_t i = 2; i < offsetof(struct page, defaults) / 8; ++i) d[i] = s[i];
    d[offsetof(struct page, lift_px) / 8] = s[offsetof(struct page, lift_px) / 8];
    d[offsetof(struct page, minus) / 8] = s[offsetof(struct page, minus) / 8];
    d[offsetof(struct page, no_prestart) / 8] = s[offsetof(struct page, no_prestart) / 8];
    d[offsetof(struct page, slideup) / 8] = s[offsetof(struct page, slideup) / 8];
    mprotect(base, ps, PROT_READ | PROT_WRITE);   // stays writable: HyperOS 3 recents islands keep a tap marker at 0x160
    LOGI("applied enable=%d style=%d radius=%d padding=%d height=%d iconsize=+%d%% indicator=%d lift=%dpx qsb=%d resize=%d iconfx=%d dockicon=%d fullanim=%d widgets=%d recentsblur=%d%% hideclear=%d hideapp=%d minus=%d noprestart=%d slideup=%d",
         enable, style, (int)n.radius, (int)n.padding, (int)n.height, (int)((n.icon_factor - 1.0) * 100 + 0.5), (int)n.ioff, (int)(n.lift_px + 0.5), (int)n.qsb, (int)n.widget_resize,
         (int)n.icon_fx, (int)n.doff, (int)n.full_anim, (int)n.widget_source, (int)(n.rblur * 100 + 0.5), (int)n.hide_clear, (int)n.hide_app, (int)n.minus, (int)n.no_prestart, (int)n.slideup);
}

static void cache_path(char *out, size_t size) {
    snprintf(out, size, "/data/user_de/%u/com.miui.home/kashi_dock.bin", (unsigned)(getuid() / 100000));
}
static int cache_load(int32_t v[F_COUNT]) {
    char path[128]; cache_path(path, sizeof path);
    FILE *f = fopen(path, "re");
    if (!f) return 0;
    int32_t t[F_COUNT + 1];
    const size_t n = fread(t, sizeof t[0], F_COUNT + 1, f);
    fclose(f);
    if (n != F_COUNT + 1 || t[0] != 0x4b443039) return 0;
    memcpy(v, t + 1, sizeof(int32_t) * F_COUNT);
    return 1;
}
static void cache_save(const int32_t v[F_COUNT]) {
    char path[128], tmp[140]; cache_path(path, sizeof path);
    snprintf(tmp, sizeof tmp, "%s.tmp", path);
    FILE *f = fopen(tmp, "we");
    if (!f) return;
    int32_t t[F_COUNT + 1]; t[0] = 0x4b443039;
    memcpy(t + 1, v, sizeof(int32_t) * F_COUNT);
    const size_t n = fwrite(t, sizeof t[0], F_COUNT + 1, f);
    fclose(f);
    if (n == F_COUNT + 1) rename(tmp, path); else unlink(tmp);
}

static int glass_load(int32_t g[GLASS_KEYS]) {
    char path[128]; cache_path(path, sizeof path);
    strcat(path, ".glass");
    FILE *f = fopen(path, "re");
    if (!f) return 0;
    const size_t n = fread(g, sizeof g[0], GLASS_KEYS, f);
    fclose(f);
    return n == GLASS_KEYS;
}
static void glass_save(const int32_t g[GLASS_KEYS]) {
    char path[128], tmp[140]; cache_path(path, sizeof path);
    strcat(path, ".glass");
    snprintf(tmp, sizeof tmp, "%s.tmp", path);
    FILE *f = fopen(tmp, "we");
    if (!f) return;
    const size_t n = fwrite(g, sizeof g[0], GLASS_KEYS, f);
    fclose(f);
    if (n == GLASS_KEYS) rename(tmp, path); else unlink(tmp);
}

static int find_launcher(struct dl_phdr_info *info, size_t size, void *out) {
    (void)size;
    const char *name = info->dlpi_name;
    const size_t len = name ? strlen(name) : 0;
    if (len >= 18 && !strcmp(name + len - 18, "libapp_launcher.so")) { *(uintptr_t *)out = info->dlpi_addr; return 1; }
    return 0;
}

static void glass_publish(struct page *page, const int32_t g[GLASS_KEYS]) {
    const long ps = sysconf(_SC_PAGESIZE);
    uintptr_t start = (uintptr_t)page + GLASS_OFFSET;
    void *base = (void *)(start & ~(uintptr_t)(ps - 1));
    if (mprotect(base, ps, PROT_READ | PROT_WRITE) != 0) return;
    for (int k = 0; k < GLASS_PARAMS; ++k) {
        const int p = kGlassFieldParam[k];
        for (int r = 0; r < 2; ++r) {
            const int base_i = r * (GLASS_PARAMS + 1);
            int v = g[base_i + 1 + p];
            v = p == 0 ? clampi(v, 0, 200) : p == 11 ? clampi(v, -200, 100) : clampi(v, -100, 200);
            const int delta = p == 0 ? v - 100 : p == 11 ? -v : v;
            volatile uint64_t *rec = (volatile uint64_t *)(start + k * 80 + r * 40);
            double factor = 1.0 + delta / 100.0, fallback = p == 0 ? 0.0 : delta / 100.0;
            uint64_t fb, fc;
            memcpy(&fc, &factor, 8); memcpy(&fb, &fallback, 8);
            rec[0] = 0;              // disable while the pair changes
            rec[1] = fc;
            rec[2] = fb;
            rec[0] = (g[base_i] != 0 && delta != 0) ? 1 : 0;
        }
    }
    mprotect(base, ps, PROT_READ | PROT_WRITE);   // stays writable: HyperOS 3 recents islands keep a tap marker at 0x160
    LOGI("glass home=%d dock=%d", g[0], g[GLASS_PARAMS + 1]);
}

static int read_settings(uintptr_t launcher, const int32_t defaults[F_COUNT], int32_t v[F_COUNT]) {
    if (!content_resolver_new || !content_resolver_delete || !settings_system_get_i32) return 0;
    void **runtime = (void **)(launcher + kashi_cfg.runtime_off);
    // Lazy global of the launcher: Arc pointer plus its state word; wait until it is initialised.
    if (!*runtime || *(volatile uint32_t *)(runtime + 1)) return 0;
    void *resolver = content_resolver_new(runtime);
    if (!resolver) return 0;
    for (int i = 0; i < F_COUNT; ++i) {
        const uint64_t r = settings_system_get_i32(resolver, kKeys[i], strlen(kKeys[i]));
        v[i] = (r & 1) ? defaults[i] : (int32_t)(r >> 32);
    }
    if (settings_secure_get_i32) {
        const uint64_t r = settings_secure_get_i32(resolver, "display_density_forced", 22);
        g_dpi = (r & 1) ? 0 : (int)(r >> 32);
    }
    if (settings_system_get_string) {
        // the launcher's own swipe-up choice: global_search / content_center / no_action (unset = default)
        static const char key[] = "launcher_slideup_gesture";
        struct kashi_rstr r = settings_system_get_string(resolver, key, sizeof key - 1);
        if (r.tag == 0) {
            // off only for values that clearly mean "none" / content center; anything else (the Rust launcher's
            // own search item value, unknown values) keeps swipe-up search as before
            static const char *const off[] = {"no_action", "none", "preference_desktop_slide_item_none",
                                              "content_center", "preference_desktop_slide_item_content"};
            int search = 1;
            for (size_t i = 0; r.ptr && i < sizeof off / sizeof off[0]; ++i)
                if (r.len == strlen(off[i]) && !memcmp(r.ptr, off[i], r.len)) search = 0;
            static char last[64] = "";
            char cur[64];
            snprintf(cur, sizeof cur, "%.*s", (int)(r.len < 60 ? r.len : 60), r.ptr ? r.ptr : "");
            if (strcmp(cur, last)) { LOGI("slideup gesture=\"%s\" -> search=%d", cur, search); strcpy(last, cur); }
            g_slide_search = search;
            // free it (abi_stable VecVTable: [0] field mask, [1] layout, [2] type id, [3] destructor)
            if (r.vt && (uintptr_t)r.vt[0] < 0x100 && r.vt[3]) ((void (*)(void *))r.vt[3])(&r.ptr);
        } else {
            g_slide_search = 1;
        }
    }
    for (int i = 0; i < GLASS_KEYS; ++i) {
        const uint64_t r = settings_system_get_i32(resolver, glass_keys[i], strlen(glass_keys[i]));
        glass_cur[i] = (r & 1) ? glass_def[i] : (int32_t)(r >> 32);
    }
    content_resolver_delete(resolver);
    return 1;
}

static void *worker(void *arg) {
    (void)arg;
    pthread_setname_np(pthread_self(), "kashi-dock");
    struct page *page = NULL;
    for (int i = 0; !page && i < 6000; ++i) {      // libapp.so is mapped shortly after this library
        page = find_page();
        if (!page) usleep(i < 2000 ? 5000 : 100000);
    }
    if (!page) { LOGI("data page not found"); return NULL; }
    int32_t defaults[F_COUNT], current[F_COUNT], v[F_COUNT];
    memcpy(defaults, page->defaults, sizeof defaults);
    _Static_assert(offsetof(struct page, defaults) == 0xc0, "page layout");
    _Static_assert(offsetof(struct page, lift_px) == 0x140, "page layout");
    _Static_assert(offsetof(struct page, no_prestart) == 0x150, "page layout");
    _Static_assert(offsetof(struct page, slideup) == 0x168, "page layout");
    memcpy(current, defaults, sizeof current);
    if (cache_load(v)) { publish(page, v); memcpy(current, v, sizeof current); }
    glass_init();
    int32_t glass_applied[GLASS_KEYS];
    memcpy(glass_applied, glass_def, sizeof glass_applied);
    if (glass_load(glass_cur)) { glass_publish(page, glass_cur); memcpy(glass_applied, glass_cur, sizeof glass_applied); }
    LOGI("ready page=%p", (void *)page);
    uintptr_t launcher = 0;
    for (unsigned tick = 0;; ++tick) {
        if (!launcher) dl_iterate_phdr(find_launcher, &launcher);
        static int slide_published = -1;
        if (launcher && read_settings(launcher, defaults, v) && (memcmp(v, current, sizeof v) || slide_published != g_slide_search)) {
            slide_published = g_slide_search;
            publish(page, v);
            cache_save(v);
            memcpy(current, v, sizeof current);
        }
        if (memcmp(glass_cur, glass_applied, sizeof glass_cur)) {
            glass_publish(page, glass_cur);
            glass_save(glass_cur);
            memcpy(glass_applied, glass_cur, sizeof glass_applied);
        }
        usleep(tick < 20 ? 500000 : 3000000);
    }
    return NULL;
}

__attribute__((constructor)) static void kashi_dock_init(void) {
    // App libraries are loaded after the spawner has forked the launcher, so a thread is safe here.
    pthread_t t;
    if (!pthread_create(&t, NULL, worker, NULL)) pthread_detach(t);
}
