/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.Color;
import java.util.List;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;

/**
 * Color constants consistent with the Hudson color palette.
 *
 * @author Kohsuke Kawaguchi
 */
public class ColorPalette {
    public static final Color RED = new Color(0xcc, 0x00, 0x03);
    public static final Color YELLOW = new Color(0xff, 0x98, 0x00);
    public static final Color BLUE = new Color(0x13, 0X83, 0X47);
    public static final Color GREY = new Color(0x96, 0x96, 0x96);
    public static final Color DARK_GREY = new Color(0x33, 0x33, 0x33);
    public static final Color LIGHT_GREY = new Color(0xcc, 0xcc, 0xcc);

    /**
     * Color list usable for generating line charts.
     */
    @SuppressFBWarnings(value = "MS_SHOULD_BE_FINAL", justification = "used in several plugins")
    public static List<Color> LINE_GRAPH = List.of(
        new Color(0xCC0000),
        new Color(0x3465a4),
        new Color(0x73d216),
        new Color(0xedd400)
    );

    /**
     * Applies {@link #LINE_GRAPH} colors to the given renderer.
     */
    public static void apply(LineAndShapeRenderer renderer) {
        int n = 0;
        for (Color c : LINE_GRAPH)
            renderer.setSeriesPaint(n++, c);
    }
}
