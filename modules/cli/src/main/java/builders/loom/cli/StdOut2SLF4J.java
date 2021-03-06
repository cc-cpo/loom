/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom.cli;

import java.io.PrintStream;

final class StdOut2SLF4J {

    private static PrintStream out;
    private static PrintStream err;

    private StdOut2SLF4J() {
    }

    static void install() {
        out = new PrintStream(new SLF4JPrintStream(false));
        System.setOut(out);

        err = new PrintStream(new SLF4JPrintStream(true));
        System.setErr(err);
    }

    static void uninstall() {
        out.flush();
        err.flush();

        OriginalStreams.reset();
    }

}
