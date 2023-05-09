package cn.sunist.project.bililivedanma.factory;

import cn.sunist.project.bililivedanma.ui.Danmaku;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class DanmaWindowFactory implements ToolWindowFactory {
    private final Danmaku danmaku = new Danmaku();

    private final ContentFactory getInstance() {
        return ApplicationManager.getApplication().getService(ContentFactory.class);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = getInstance();
        Content content = contentFactory.createContent(danmaku.getComponent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
