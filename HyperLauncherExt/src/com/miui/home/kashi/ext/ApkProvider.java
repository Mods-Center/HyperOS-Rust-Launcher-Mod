package com.miui.home.kashi.ext;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Read-only access to an installed app's base APK for the share sheet: content://com.miui.home.kashi.ext.apk/<package>.
 * Not exported; readers get a one-shot grant from ShareActivity.
 */
public class ApkProvider extends ContentProvider {
    static final String AUTHORITY = "com.miui.home.kashi.ext.apk";

    static Uri uriFor(String pkg) { return new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(pkg).build(); }

    private File apk(Uri uri) throws FileNotFoundException {
        try {
            String pkg = uri.getLastPathSegment();
            ApplicationInfo ai = getContext().getPackageManager().getApplicationInfo(pkg, 0);
            File f = new File(ai.publicSourceDir != null ? ai.publicSourceDir : ai.sourceDir);
            if (!f.canRead()) throw new FileNotFoundException(f.getPath());
            return f;
        } catch (FileNotFoundException e) { throw e; }
        catch (Exception e) { throw new FileNotFoundException(String.valueOf(uri)); }
    }

    private String displayName(Uri uri) {
        String pkg = uri.getLastPathSegment();
        try {
            android.content.pm.PackageManager pm = getContext().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(pkg, 0);
            String name = String.valueOf(pi.applicationInfo.loadLabel(pm)).replaceAll("[\\\\/:*?\"<>|]", "_").trim();
            return name + (pi.versionName != null ? "_" + pi.versionName : "") + ".apk";
        } catch (Exception e) { return pkg + ".apk"; }
    }

    @Override public boolean onCreate() { return true; }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("read only");
        return ParcelFileDescriptor.open(apk(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        String[] cols = projection != null ? projection : new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor c = new MatrixCursor(cols);
        Object[] row = new Object[cols.length];
        long size = 0;
        try { size = apk(uri).length(); } catch (Exception ignored) { }
        for (int i = 0; i < cols.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) row[i] = displayName(uri);
            else if (OpenableColumns.SIZE.equals(cols[i])) row[i] = size;
        }
        c.addRow(row);
        return c;
    }

    @Override public String getType(Uri uri) { return "application/vnd.android.package-archive"; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
