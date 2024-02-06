package org.yaml.yamlscript;

import com.sun.jna.ptr.PointerByReference;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Interface with the shared libyamlscript library
 *
 */
public class YAMLScript
{
    public static String YAML_SCRIPT_VERSION = "0.1.36";

    public static Object load(String ysCode)
    {
        return loadJSON(ysCode).get("data");
    }

    public static JSONObject loadObject(String ysCode)
    {
        return loadJSON(ysCode).getJSONObject("data");
    }

    public static JSONArray loadArray(String ysCode)
    {
        return loadJSON(ysCode).getJSONArray("data");
    }

    public static int loadInt(String ysCode)
    {
        return loadJSON(ysCode).getInt("data");
    }

    public static float loadFloat(String ysCode)
    {
        return loadJSON(ysCode).getFloat("data");
    }

    public static double loadDouble(String ysCode)
    {
        return loadJSON(ysCode).getDouble("data");
    }

    public static boolean loadBoolean(String ysCode)
    {
        return loadJSON(ysCode).getBoolean("data");
    }

    public static long loadLong(String ysCode)
    {
        return loadJSON(ysCode).getLong("data");
    }

    public static BigInteger loadBigInteger(String ysCode)
    {
        return loadJSON(ysCode).getBigInteger("data");
    }

    public static BigDecimal loadBigDecimal(String ysCode)
    {
        return loadJSON(ysCode).getBigDecimal("data");
    }

    public static JSONObject loadJSON(String code)
    {
        return new YAMLScript().evaluate(code);
    }

    private final ILibYAMLScript libyamlscript;
    private final PointerByReference isolateRef;

    public YAMLScript()
    {
        this.libyamlscript = LibYAMLScript.library();
        this.isolateRef = new PointerByReference();
    }

    public String getRAWResult(String ysCode) throws RuntimeException
    {
        PointerByReference threadRef = new PointerByReference();
        libyamlscript.graal_create_isolate(null, isolateRef, threadRef);

        String jsonData = libyamlscript.load_ys_to_json(threadRef.getValue(), ysCode);

        int result = libyamlscript.graal_tear_down_isolate(threadRef.getValue());
        if (result != 0) throw new RuntimeException("Failed to tear down isolate");

        return jsonData;
    }

    public JSONObject evaluate(String ysCode)
    {
        JSONObject jsonData = new JSONObject(getRAWResult(ysCode));

        if (jsonData.has("error") && !jsonData.isNull("error")) {
            String error = jsonData.getString("error");
            throw new RuntimeException(error);
        }

        return jsonData;
    }

}
