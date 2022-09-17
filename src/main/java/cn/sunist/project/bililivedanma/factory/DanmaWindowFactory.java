package cn.sunist.project.bililivedanma.factory;

import cn.sunist.project.bililivedanma.ui.Danmaku;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class DanmaWindowFactory implements ToolWindowFactory {
    private final Danmaku danmaku = new Danmaku();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(danmaku.getComponent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
