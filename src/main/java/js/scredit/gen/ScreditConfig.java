package js.scredit.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class ScreditConfig implements AbstractData {

  public File projectDir() {
    return mProjectDir;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "project_dir";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mProjectDir.toString());
    return m;
  }

  @Override
  public ScreditConfig build() {
    return this;
  }

  @Override
  public ScreditConfig parse(Object obj) {
    return new ScreditConfig((JSMap) obj);
  }

  private ScreditConfig(JSMap m) {
    {
      mProjectDir = Files.DEFAULT;
      String x = m.opt(_0, (String) null);
      if (x != null) {
        mProjectDir = new File(x);
      }
    }
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof ScreditConfig))
      return false;
    ScreditConfig other = (ScreditConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mProjectDir.equals(other.mProjectDir)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mProjectDir.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected File mProjectDir;
  protected int m__hashcode;

  public static final class Builder extends ScreditConfig {

    private Builder(ScreditConfig m) {
      mProjectDir = m.mProjectDir;
    }

    @Override
    public Builder toBuilder() {
      return this;
    }

    @Override
    public int hashCode() {
      m__hashcode = 0;
      return super.hashCode();
    }

    @Override
    public ScreditConfig build() {
      ScreditConfig r = new ScreditConfig();
      r.mProjectDir = mProjectDir;
      return r;
    }

    public Builder projectDir(File x) {
      mProjectDir = (x == null) ? Files.DEFAULT : x;
      return this;
    }

  }

  public static final ScreditConfig DEFAULT_INSTANCE = new ScreditConfig();

  private ScreditConfig() {
    mProjectDir = Files.DEFAULT;
  }

}
