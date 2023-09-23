import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.javascript.nodejs.interpreter.wsl.WslNodeInterpreter
import com.intellij.lang.javascript.service.JSLanguageServiceUtil
import com.intellij.execution.ExecutionException
import com.intellij.javascript.nodejs.interpreter.NodeCommandLineConfigurator

class AngelscriptLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
        if (file.extension != "as") return

        val node = NodeJsInterpreterManager.getInstance(project).interpreter;
        if (node !is NodeJsLocalInterpreter && node !is WslNodeInterpreter) return;

        serverStarter.ensureServerStarted(AngelscriptServerDescriptor(project))
    }
}

private class AngelscriptServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Angelscript") {
    override fun createCommandLine(): GeneralCommandLine {
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter
        if (interpreter !is NodeJsLocalInterpreter && interpreter !is WslNodeInterpreter) {
            throw ExecutionException("Invalid NodeJs interpreter");
        }

        val lsp = JSLanguageServiceUtil.getPluginDirectory(javaClass, "language-server/src/server.ts")
        if (lsp == null || !lsp.exists()) {
            throw ExecutionException("Lsp not found")
        }

        return GeneralCommandLine().apply {
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(Charsets.UTF_8)
            addParameter(lsp.path)
            addParameter("--stdio")

            NodeCommandLineConfigurator.find(interpreter)
                .configure(this, NodeCommandLineConfigurator.defaultOptions(project))
        }

    }

    override fun isSupportedFile(file: VirtualFile): Boolean {
        return file.extension == "as"
    }
}
