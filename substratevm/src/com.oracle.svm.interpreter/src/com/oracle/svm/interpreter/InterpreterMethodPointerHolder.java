/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.interpreter;

import com.oracle.svm.core.BuildPhaseProvider;
import com.oracle.svm.core.Uninterruptible;
import com.oracle.svm.core.heap.UnknownPrimitiveField;

import jdk.graal.compiler.api.replacements.Fold;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.c.function.CFunctionPointer;

public class InterpreterMethodPointerHolder {
    @Fold
    public static InterpreterMethodPointerHolder singleton() {
        return ImageSingletons.lookup(InterpreterMethodPointerHolder.class);
    }

    @UnknownPrimitiveField(availability = BuildPhaseProvider.AfterCompilation.class) private CFunctionPointer methodNotCompiledFtnPtr;

    @Uninterruptible(reason = "Called from uninterruptible code.", mayBeInlined = true)
    public static CFunctionPointer getMethodNotCompiledHandler() {
        CFunctionPointer ptr = singleton().methodNotCompiledFtnPtr;
        assert ptr.rawValue() != 0;
        return ptr;
    }

    @Platforms(Platform.HOSTED_ONLY.class)
    public static void setMethodNotCompiledHandler(CFunctionPointer ftnptr) {
        assert singleton().methodNotCompiledFtnPtr == null;
        singleton().methodNotCompiledFtnPtr = ftnptr;
    }
}
