using System.Runtime.InteropServices;

namespace YAMLScript.Native;

internal static class YAMLScriptNative
{
    private const string LibraryName = "libys";

    [DllImport(LibraryName)]
    public static extern int graal_create_isolate(
        IntPtr params_ptr,
        IntPtr isolate_ptr,
        ref IntPtr isolate_thread_ptr);

    [DllImport(LibraryName)]
    public static extern int graal_tear_down_isolate(IntPtr isolate_thread_ptr);

    [DllImport(LibraryName)]
    public static extern IntPtr load_ys_to_json(
        IntPtr isolate_thread_ptr,
        [MarshalAs(UnmanagedType.LPStr)] string yaml);
}
