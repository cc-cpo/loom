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

package builders.loom.api;

import java.util.Set;
import java.util.function.Supplier;

public interface TaskRegistry {

    void registerTask(final String pluginName, String taskName, Supplier<Task> taskSupplier,
                      Set<String> providedProducts, Set<String> usedProducts);

    void registerGoal(final String pluginName, String goalName, Set<String> usedProducts);

}
