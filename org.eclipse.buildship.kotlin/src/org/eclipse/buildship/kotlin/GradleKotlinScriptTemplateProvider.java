package org.eclipse.buildship.kotlin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.JavaRuntime;
import org.jetbrains.kotlin.core.model.ScriptTemplateProviderEx;
import org.jetbrains.kotlin.script.KotlinScriptExternalDependencies;
import org.jetbrains.kotlin.script.ScriptContents;
import org.jetbrains.kotlin.script.ScriptContents.Position;
import org.jetbrains.kotlin.script.ScriptDependenciesResolver;
import org.jetbrains.kotlin.script.ScriptTemplateDefinition;
import org.jetbrains.kotlin.script.ScriptTemplateProvider;
import org.osgi.framework.Bundle;

import kotlin.Unit;
import kotlin.jvm.functions.Function3;

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
		return new ScriptDependenciesResolver() {
			ScriptDependenciesResolver delegateResolver = loadAnnotationResolver();
			
			@Override
			public Future<KotlinScriptExternalDependencies> resolve(
					ScriptContents content,
					Map<String, ? extends Object> environment,
					Function3<? super ReportSeverity, ? super String, ? super Position, Unit> report,
					KotlinScriptExternalDependencies dependencies) {
				
				// NOTE: resolve() method is only executed on script file opening. There're no updates on file modification (KT-13857). 
				if (content.getText().toString().contains("standard")) {
					return delegateResolver.resolve(content, environment, report, dependencies);
				}
				
				return null;
			}
		};
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
	
	private ScriptDependenciesResolver loadAnnotationResolver() {
		List<URL> urls = new ArrayList<>();
		try {
			for (String entry : getTemplateClassClasspath()) {
				urls.add(new File(entry).toURI().toURL());
			}
			
			URLClassLoader loader = new URLClassLoader(
					urls.toArray(new URL[0]),
					this.getClass().getClassLoader());
			
			Class<?> klass = loader.loadClass(getTemplateClassName());
			ScriptTemplateDefinition scriptTemplateDefinition = klass.getAnnotation(ScriptTemplateDefinition.class);
			Class<? extends ScriptDependenciesResolver> resolverClass = scriptTemplateDefinition.resolver();
			ScriptDependenciesResolver resolver = resolverClass.getConstructor().newInstance();
			
			return resolver;
		} catch (MalformedURLException | ClassNotFoundException | InstantiationException | 
				IllegalAccessException | IllegalArgumentException | InvocationTargetException | 
				NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
}