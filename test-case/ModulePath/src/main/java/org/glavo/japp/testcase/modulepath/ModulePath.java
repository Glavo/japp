/*
 * Copyright (C) 2023 Glavo
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
package org.glavo.japp.testcase.modulepath;

import com.google.gson.Gson;
import org.apache.commons.lang3.ObjectUtils;

public final class ModulePath {
    public static void main(String[] args) {
        System.out.println(ModulePath.class.getResource("ModulePath.class"));
        System.out.println(Gson.class.getResource("Gson.class"));
        System.out.println(ObjectUtils.class.getResource("ObjectUtils.class"));
    }
}
