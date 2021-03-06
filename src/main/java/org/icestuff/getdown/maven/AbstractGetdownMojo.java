package org.icestuff.getdown.maven;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Make deployable Java apps.
 */
public abstract class AbstractGetdownMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    protected PluginDescriptor plugin;

    /**
     * The Sign Config.
     */
    @Parameter
    protected SignConfig sign;

    /**
     * Sign tool.
     */
    @Component
    protected SignTool signTool;

    /**
     * Enable verbose output.
     */
    @Parameter(defaultValue = "false")
    protected boolean verbose;

    @Parameter
    protected UiConfig ui = new UiConfig();

    /**
     * The path where the resources are placed within the getdown structure.
     */
    @Parameter(defaultValue = "")
    protected String resourceSetsPath;

    /**
     * Compile class-path elements used to search for the keystore (if kestore
     * location was prefixed by {@code classpath:}).
     * 
     * @since 1.0-beta-4
     */
    @Parameter(defaultValue = "${project.compileClasspathElements}", required = true, readonly = true)
    protected List<?> compileClassPath;

    protected abstract File getWorkDirectory();

    protected void initSign() throws MojoExecutionException {
	if (sign != null) {
	    try {
		ClassLoader loader = getCompileClassLoader();
		sign.init(getWorkDirectory(), getLog().isDebugEnabled(),
			signTool, loader);
	    } catch (MalformedURLException e) {
		throw new MojoExecutionException(
			"Could not create classloader", e);
	    }
	}
    }

    private ClassLoader getCompileClassLoader() throws MalformedURLException {
	URL[] urls = new URL[compileClassPath.size()];
	for (int i = 0; i < urls.length; i++) {
	    String spec = compileClassPath.get(i).toString();
	    URL url = new File(spec).toURI().toURL();
	    urls[i] = url;
	}
	return new URLClassLoader(urls);
    }

    protected void writeUIResources(PrintWriter writer) {
	if (ui.backgroundImage != null) {
	    writer.println(String.format("resource = %s",
		    getResourceSetPath(ui.backgroundImage)));
	}
	if (ui.errorBackground != null) {
	    writer.println(String.format("resource = %s",
		    getResourceSetPath(ui.errorBackground)));
	}
	if (ui.icons != null) {
	    for (String i : ui.icons) {
		writer.println(String.format("resource = %s",
			getResourceSetPath(i)));
	    }
	}
	if (ui.progressImage != null) {
	    writer.println(String.format("resource = %s",
		    getResourceSetPath(ui.progressImage)));
	}
    }

    protected File getResourceSetsDirectory() {
	if (resourceSetsPath != null) {
	    return new File(getWorkDirectory(), resourceSetsPath);
	}
	return getWorkDirectory();
    }

    protected void copyUIResources() throws MojoExecutionException {
	if (ui.backgroundImage != null) {
	    getLog().info(
		    String.format("Using background image %s",
			    ui.backgroundImage));
	    Util.copyFileToDir(new File(ui.backgroundImage),
		    getResourceSetsDirectory());
	}
	if (ui.errorBackground != null) {
	    getLog().info(
		    String.format("Using error background %s",
			    ui.backgroundImage));
	    Util.copyFileToDir(new File(ui.errorBackground),
		    getResourceSetsDirectory());
	}
	if (ui.progressImage != null) {
	    getLog().info(
		    String.format("Using progress image %s", ui.progressImage));
	    Util.copyFileToDir(new File(ui.progressImage),
		    getResourceSetsDirectory());
	}
	if (ui.icons != null) {
	    for (String i : ui.icons) {
		getLog().info(String.format("Using icon %s", i));
		Util.copyFileToDir(new File(i), getResourceSetsDirectory());
	    }
	}
	if (ui.macDockIcon != null) {
	    getLog().info(
		    String.format("Using Mac dock icon %s", ui.macDockIcon));
	    Util.copyFileToDir(new File(ui.macDockIcon),
		    getResourceSetsDirectory());
	}
    }

    protected void writeUIConfiguration(PrintWriter writer) {
	writer.println("# UI Configuration");
	if (ui.backgroundImage != null) {
	    writer.println(String.format("ui.background_image = %s",
		    getResourceSetPath(ui.backgroundImage)));
	}
	if (ui.errorBackground != null) {
	    writer.println(String.format("ui.error_background = %s",
		    getResourceSetPath(ui.errorBackground)));
	}
	if (ui.icons != null) {
	    for (String i : ui.icons) {
		writer.println(String.format("ui.icon = %s",
			getResourceSetPath(i)));
	    }
	}
	if (ui.progressImage != null) {
	    writer.println(String.format("ui.progress_image = %s",
		    getResourceSetPath(ui.progressImage)));
	}
	if (ui.macDockIcon != null) {
	    writer.println(String.format("ui.mac_dock_icon = %s",
		    getResourceSetPath(ui.macDockIcon)));
	}
    }

    protected String getResourceSetPath(String name) {
	int idx = name.lastIndexOf('/');
	if (idx != -1) {
	    name = name.substring(idx + 1);
	}
	if (StringUtils.isNotBlank(resourceSetsPath)) {
	    return resourceSetsPath + "/" + name;
	}
	return name;
    }

    /**
     * Computes the path for a file relative to a given base, or fails if the
     * only shared directory is the root and the absolute form is better.
     * 
     * @param base
     *            File that is the base for the result
     * @param name
     *            File to be "relativized"
     * @return the relative name
     * @throws IOException
     *             if files have no common sub-directories, i.e. at best share
     *             the root prefix "/" or "C:\"
     * 
     *             http://stackoverflow.com/questions/204784/how-to-construct-a-
     *             relative-path-in-java-from-two-absolute-paths-or-urls
     */

    public static String getRelativePath(File base, File name)
	    throws IOException {
	File parent = base.getParentFile();

	if (parent == null) {
	    throw new IOException("No common directory");
	}

	String bpath = base.getCanonicalPath();
	String fpath = name.getCanonicalPath();

	if (fpath.startsWith(bpath)) {
	    return fpath.substring(bpath.length() + 1);
	} else {
	    return (".." + File.separator + getRelativePath(parent, name));
	}
    }

    /**
     * Log as info when verbose or info is enabled, as debug otherwise.
     * 
     * @param msg
     *            the message to display
     */
    protected void verboseLog(String msg) {
	if (verbose) {
	    getLog().info(msg);
	} else {
	    getLog().debug(msg);
	}
    }

}
