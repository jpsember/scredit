package js.scredit;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import static js.base.Tools.*;

import js.geometry.IPoint;
import js.widget.SwingWidgetManager;
import js.widget.Widget;
import js.widget.WidgetManager;

public class InfoPanel extends JPanel {

  public void opening(Project project) {
    if (!todo("!restore widget state map from project somehow")) {
      // mWidgetManager.setStateMap(project.widgetStateMap());
      mWidgetManager.restoreWidgetValues();
    }
  }

  public InfoPanel(ScrEdit editor) {
    mEditor = editor;
    setBorder(BorderFactory.createRaisedBevelBorder());

    WidgetManager m = new SwingWidgetManager();
    // Use the InfoPanel as the outermost container
    m.setPendingContainer(this);

    m.columns(".x").open();
    {
      m.addLabel("Script:");
      mFilePath = m.monospaced().addText();
      mMessageField = m.skip().monospaced().addText();
    }
    m.addVertGrow();
    m.close();
    m.setPrepared(true);
    mWidgetManager = m;
  }

  public void refresh() {
    ScriptWrapper script = mEditor.currentScript();

    String scriptDisplay = "";
    if (!script.isNone()) {
      Project project = mEditor.currentProject();
      StringBuilder sb = new StringBuilder();
      sb.append(project.scriptIndex());
      sb.append("/");
      sb.append(project.scriptCount());
      sb.append(" ");
      String nameOnly = script.name();
      sb.append(nameOnly);

      {
        IPoint imageSize = script.imageSize();
        if (imageSize.nonZero()) {
          sb.append(" (" + imageSize.x + " x " + imageSize.y + ")");
        }
      }

      scriptDisplay = sb.toString();

    }
    mFilePath.setText(scriptDisplay);
  }

  public void setMessage(String text) {
    text = nullToEmpty(text);
    if (text.startsWith("!")) {
      text = "*** " + text.substring(1);
      mErrorTime = System.currentTimeMillis();
    } else {
      if (System.currentTimeMillis() - mErrorTime < 20000)
        return;
    }
    mMessageField.setText(text);
  }

  private final ScrEdit mEditor;
  private final WidgetManager mWidgetManager;
  private long mErrorTime;
  private Widget mFilePath;
  private Widget mMessageField;
}
