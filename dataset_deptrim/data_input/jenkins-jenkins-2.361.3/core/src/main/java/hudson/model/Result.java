/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi
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

package hudson.model;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.cli.declarative.OptionHandlerExtension;
import hudson.init.Initializer;
import hudson.util.EditDistance;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.Converter;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.export.CustomExportedBean;

/**
 * The build outcome.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Result implements Serializable, CustomExportedBean {
    /**
     * The build had no errors.
     */
    public static final @NonNull Result SUCCESS = new Result("SUCCESS", BallColor.BLUE, 0, true);
    /**
     * The build had some errors but they were not fatal.
     * For example, some tests failed.
     */
    public static final @NonNull Result UNSTABLE = new Result("UNSTABLE", BallColor.YELLOW, 1, true);
    /**
     * The build had a fatal error.
     */
    public static final @NonNull Result FAILURE = new Result("FAILURE", BallColor.RED, 2, true);
    /**
     * The module was not built.
     * <p>
     * This status code is used in a multi-stage build (like maven2)
     * where a problem in earlier stage prevented later stages from building.
     */
    public static final @NonNull Result NOT_BUILT = new Result("NOT_BUILT", BallColor.NOTBUILT, 3, false);
    /**
     * The build was manually aborted.
     *
     * If you are catching {@link InterruptedException} and interpreting it as {@link #ABORTED},
     * you should check {@link Executor#abortResult()} instead (starting 1.417.)
     */
    public static final @NonNull Result ABORTED = new Result("ABORTED", BallColor.ABORTED, 4, false);

    private final @NonNull String name;

    /**
     * Bigger numbers are worse.
     */
    public final /* @java.annotation.Nonnegative */ int ordinal;

    /**
     * Default ball color for this status.
     */
    public final @NonNull BallColor color;

    /**
     * Is this a complete build - i.e. did it run to the end (not aborted)?
     * @since 1.526
     */
    public final boolean completeBuild;

    private Result(@NonNull String name, @NonNull BallColor color, /*@java.annotation.Nonnegative */int ordinal, boolean complete) {
        this.name = name;
        this.color = color;
        this.ordinal = ordinal;
        this.completeBuild = complete;
    }

    /**
     * Combines two {@link Result}s and returns the worse one.
     */
    public @NonNull Result combine(@NonNull Result that) {
        if (this.ordinal < that.ordinal)
            return that;
        else
            return this;
    }

    /**
     * Combines two {@link Result}s and returns the worse one.
     * <p>
     * This method is null-safe (any {@link Result} is "worse" than {@code null}, and {@code null} is returned if both
     * parameters are {@code null}).
     *
     * @param r1
     *      a result (may be {@code null})
     * @param r2
     *      a result (may be {@code null})
     * @return the worst result (may be {@code null})
     * @since 2.257
     */
    public static Result combine(Result r1, Result r2) {
        if (r1 == null) {
            return r2;
        } else if (r2 == null) {
            return r1;
        } else {
            return r1.combine(r2);
        }
    }

    public boolean isWorseThan(@NonNull Result that) {
        return this.ordinal > that.ordinal;
    }

    public boolean isWorseOrEqualTo(@NonNull Result that) {
        return this.ordinal >= that.ordinal;
    }

    public boolean isBetterThan(@NonNull Result that) {
        return this.ordinal < that.ordinal;
    }

    public boolean isBetterOrEqualTo(@NonNull Result that) {
        return this.ordinal <= that.ordinal;
    }

    /**
     * Is this a complete build - i.e. did it run to the end (not aborted)?
     * @since 1.526
     */
    public boolean isCompleteBuild() {
        return this.completeBuild;
    }

    @Override
    public @NonNull String toString() {
        return name;
    }

    @Override
    public @NonNull String toExportedObject() {
        return name;
    }

    public static @NonNull Result fromString(@NonNull String s) {
        for (Result r : all)
            if (s.equalsIgnoreCase(r.name))
                return r;
        return FAILURE;
    }

    private static @NonNull List<String> getNames() {
        List<String> l = new ArrayList<>();
        for (Result r : all)
            l.add(r.name);
        return l;
    }

    // Maintain each Result as a singleton deserialized (like build result from an agent node)
    private Object readResolve() {
        for (Result r : all)
            if (ordinal == r.ordinal)
                return r;
        return FAILURE;
    }

    private static final long serialVersionUID = 1L;

    private static final Result[] all = new Result[] {SUCCESS, UNSTABLE, FAILURE, NOT_BUILT, ABORTED};

    public static final SingleValueConverter conv = new AbstractSingleValueConverter() {
        @Override
        public boolean canConvert(Class clazz) {
            return clazz == Result.class;
        }

        @Override
        public Object fromString(String s) {
            return Result.fromString(s);
        }
    };

    @OptionHandlerExtension
    public static final class OptionHandlerImpl extends OptionHandler<Result> {
        public OptionHandlerImpl(CmdLineParser parser, OptionDef option, Setter<? super Result> setter) {
            super(parser, option, setter);
        }

        @Override
        public int parseArguments(Parameters params) throws CmdLineException {
            String param = params.getParameter(0);
            Result v = fromString(param.replace('-', '_'));
            if (v == FAILURE) {
                throw new CmdLineException(owner, "No such status '" + param + "'. Did you mean " +
                        EditDistance.findNearest(param.replace('-', '_').toUpperCase(), getNames()));
            }
            setter.addValue(v);
            return 1;
        }

        @Override
        public String getDefaultMetaVariable() {
            return "STATUS";
        }
    }

    @Initializer
    public static void init() {
        Stapler.CONVERT_UTILS.register(new Converter() {
            @Override
            public Object convert(Class type, Object value) {
                return Result.fromString(value.toString());
            }
        }, Result.class);
    }
}
