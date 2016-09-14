package org.eclipse.buildship.kotlin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.JavaRuntime;
import org.jetbrains.kotlin.core.model.ScriptTemplateProviderEx;
import org.jetbrains.kotlin.script.ScriptDependenciesResolver;
import org.jetbrains.kotlin.script.ScriptTemplateProvider;
import org.osgi.framework.Bundle;

public class GradleKotlinScriptTemplateProvider implements ScriptTemplateProviderEx {
	private static String[] classpathEntries = new String[] {
				"/bin",
				"/lib/gradle-core-3.0-20160720184418+0000.jar",
				"/lib/gradle-script-kotlin-0.3.0.jar",
				"/lib/gradle-tooling-api-3.0-20160720184418+0000.jar", 
				"/lib/slf4j-api-1.7.10.jar"
	};

	// NOTE: getDependenciesClasspath -> getTemplateClassClasspath method rename
	@Override
	public Iterable<String> getTemplateClassClasspath() {
		Bundle pluginBundle = Platform.getBundle(Activator.PLUGIN_ID);
		ArrayList<String> result = new ArrayList<String>();
		for (String path : classpathEntries) {
			try {
				result.add(FileLocator.toFileURL(pluginBundle.getEntry(path)).getFile());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		return result;
	}

	@Override
	public String getTemplateClassName() {
		// NOTE: String is now returned
		return "org.eclipse.buildship.kotlin.KotlinBuildScript";
	}

	@Override
	public Map<String, Object> getEnvironment(IFile file) {
		HashMap<String, Object> environment = new HashMap<String, Object>();
		environment.put("rtPath", rtPath());
		environment.put("rootProject", file.getProject().getLocation().toFile());
		return environment;
	}
	
	@Override
	public ScriptDependenciesResolver getResolver() {
		return null;
	}

	@Override
	public boolean isApplicable(IFile file) {
		IProject project = file.getProject();
		return file.getName().equals("build.gradle.kts");
	}
	
	private List<File> rtPath() {
		File rtJar = new File(JavaRuntime.getDefaultVMInstall().getInstallLocation(), "jre/lib/rt.jar");
		if (rtJar.exists()) {
			return Arrays.asList(rtJar);
		} 
		
		return Collections.emptyList();
	}
}