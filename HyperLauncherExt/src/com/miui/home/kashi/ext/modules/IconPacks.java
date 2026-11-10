package com.miui.home.kashi.ext.modules;

import com.miui.home.kashi.ext.L;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Converts a third-party icon pack (ADW / Nova appfilter format) into a MIUI/HyperOS theme "icons" package:
 * a zip of res/drawable-xxhdpi/&lt;package&gt;.png (+ &lt;activity class&gt;.png) that the launcher's theme engine
 * reads from /data/system/theme/icons.
 */
public final class IconPacks {
    static final String[] PACK_ACTIONS = {"org.adw.launcher.THEMES", "com.novalauncher.THEME",
            "com.teslacoilsw.launcher.THEME", "com.gau.go.launcherex.theme", "com.anddoes.launcher.THEME"};
    static final int SIZE = 192;   // xxhdpi theme icon
    static final String DEFAULT_ICONS = "/product/media/theme/default/icons";
    public static final String THEME_ICONS = "/data/system/theme/icons";

    /**
     * True when this app may write the theme folder itself: stock HyperOS only lets system daemons and Theme Manager
     * write theme_data_file, but a ROM can allow priv_app/platform_app (see the repo README). Writes a probe file.
     */
    public static boolean canWriteThemeDirect() {
        File probe = new File(new File(THEME_ICONS).getParentFile(), ".kashi_probe");
        try (java.io.FileOutputStream o = new java.io.FileOutputStream(probe)) { o.write(0); }
        catch (Exception e) { return false; }
        probe.delete();
        return true;
    }

    public static final class Pack { public String pkg, label; public Drawable icon; }

    public static List<Pack> installed(Context c) {
        PackageManager pm = c.getPackageManager();
        LinkedHashMap<String, Pack> out = new LinkedHashMap<>();
        for (String action : PACK_ACTIONS)
            for (ResolveInfo ri : pm.queryIntentActivities(new Intent(action), PackageManager.GET_META_DATA)) {
                String pkg = ri.activityInfo.packageName;
                if (out.containsKey(pkg)) continue;
                Pack p = new Pack();
                p.pkg = pkg;
                p.label = ri.activityInfo.applicationInfo.loadLabel(pm).toString();
                p.icon = ri.activityInfo.applicationInfo.loadIcon(pm);
                out.put(pkg, p);
            }
        return new ArrayList<>(out.values());
    }

    public interface Progress { void step(int done, int total); }

    /** Builds the theme icons zip; returns the number of apps that received a pack icon. */
    public static int build(Context c, String packPkg, File zipOut, Progress progress) throws Exception {
        PackageManager pm = c.getPackageManager();
        Resources res = pm.getResourcesForApplication(packPkg);
        Map<String, String> byComponent = new HashMap<>(), byPackage = new HashMap<>();
        parseAppFilter(c, res, packPkg, byComponent, byPackage);
        if (byComponent.isEmpty()) throw new Exception(L.s(c, "iconpack_no_appfilter"));

        Intent launch = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(launch, 0);
        int themed = 0, i = 0;
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipOut))) {
            java.util.Set<String> written = new java.util.HashSet<>();
            // keep the stock icon transform (mask / scale) for apps the pack does not cover
            try (java.util.zip.ZipFile def = new java.util.zip.ZipFile(DEFAULT_ICONS)) {
                for (java.util.Enumeration<? extends ZipEntry> e = def.entries(); e.hasMoreElements(); ) {
                    ZipEntry z = e.nextElement();
                    if (z.isDirectory()) continue;
                    zip.putNextEntry(new ZipEntry(z.getName()));
                    try (InputStream in = def.getInputStream(z)) { in.transferTo(zip); }
                    zip.closeEntry();
                }
            } catch (Exception ignored) { }
            for (ResolveInfo ri : apps) {
                progress.step(++i, apps.size());
                String pkg = ri.activityInfo.packageName, cls = ri.activityInfo.name;
                String name = byComponent.get(pkg + "/" + cls);
                if (name == null) name = byPackage.get(pkg);
                if (name == null) continue;
                int id = res.getIdentifier(name, "drawable", packPkg);
                if (id == 0) id = res.getIdentifier(name, "mipmap", packPkg);
                if (id == 0) continue;
                Drawable d;
                try { d = res.getDrawableForDensity(id, 480, null); } catch (Exception e) { continue; }
                if (d == null) continue;
                byte[] png = render(d);
                // ThemeIconLoader names: class (if inside the package) or package#class, then the package fallback
                String clsName = cls.startsWith(pkg) ? cls : pkg + "#" + cls;
                for (String file : new String[] {clsName, pkg}) {
                    if (!written.add(file)) continue;
                    zip.putNextEntry(new ZipEntry("res/drawable-xxhdpi/" + file + ".png"));
                    zip.write(png);
                    zip.closeEntry();
                }
                themed++;
            }
        }
        return themed;
    }

    private static byte[] render(Drawable d) {
        Bitmap b = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        d.setBounds(0, 0, SIZE, SIZE);
        d.draw(cv);
        java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, o);
        b.recycle();
        return o.toByteArray();
    }

    private static void parseAppFilter(Context c, Resources res, String pkg, Map<String, String> byComponent,
                                       Map<String, String> byPackage) throws Exception {
        XmlPullParser parser;
        int xmlId = res.getIdentifier("appfilter", "xml", pkg);
        InputStream stream = null;
        if (xmlId != 0) parser = res.getXml(xmlId);
        else {
            stream = c.createPackageContext(pkg, 0).getAssets().open("appfilter.xml");
            parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(stream, "utf-8");
        }
        Pattern comp = Pattern.compile("ComponentInfo\\{([^/}]+)/([^}]+)\\}");
        try {
            for (int ev = parser.getEventType(); ev != XmlPullParser.END_DOCUMENT; ev = parser.next()) {
                if (ev != XmlPullParser.START_TAG || !"item".equals(parser.getName())) continue;
                String component = parser.getAttributeValue(null, "component"), drawable = parser.getAttributeValue(null, "drawable");
                if (component == null || drawable == null) continue;
                Matcher m = comp.matcher(component);
                if (!m.find()) continue;
                String p = m.group(1), cls = m.group(2);
                if (cls.startsWith(".")) cls = p + cls;
                byComponent.putIfAbsent(p + "/" + cls, drawable);
                byPackage.putIfAbsent(p, drawable);
            }
        } finally {
            if (stream != null) stream.close();
        }
    }

    static ComponentName none() { return null; }
}
