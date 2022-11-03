/*
 * Copyright (c) 2011 CloudBees, Inc.
 * All rights reserved.
 * The copyrights to the contents of this file are licensed under the MIT License (http://www.opensource.org/licenses/mit-license.php)
 */

package hudson.security.csrf;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Allows plugins to define exceptions to the CSRF protection filter.
 *
 * Please note that Jenkins 2.96 and newer accepts HTTP POST requests without CSRF crumb, if
 * HTTP Basic authentication uses an API token instead of a password, so many use cases
 * (simple API clients that support authentication but not obtaining a crumb) should be obsolete.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.446
 */
public abstract class CrumbExclusion implements ExtensionPoint {
    /**
     * This method is called for every incoming POST request.
     *
     * @return
     *      true to indicate that the callee had processed this request
     *      (for example by reporting an error, or by executing the rest of the chain.)
     */
    public abstract boolean process(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException;

    public static ExtensionList<CrumbExclusion> all() {
        return ExtensionList.lookup(CrumbExclusion.class);
    }
}
