package ccm.utils;

public class CcmAppUtils {
  private final static String APP_NAME = "ISL-CCM";

static public String getAppName() {
    return APP_NAME;
}

static public String getAppComponentName(Object appComponent) {
    return appComponent.getClass().getSimpleName();
  }
}
