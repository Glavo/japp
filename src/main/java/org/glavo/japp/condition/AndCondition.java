/*
 * Copyright 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.condition;

import org.glavo.japp.platform.JAppRuntimeContext;

import java.util.Iterator;
import java.util.List;

public class AndCondition implements Condition {
    private final List<Condition> conditions;

    public AndCondition(List<Condition> conditions) {
        assert conditions.size() >= 2;
        this.conditions = conditions;
    }

    @Override
    public boolean test(JAppRuntimeContext context) {
        for (Condition condition : conditions) {
            if (!condition.test(context)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        Iterator<Condition> it = conditions.iterator();
        res.append("and(");
        res.append(it.next());
        while (it.hasNext()) {
            res.append(", ").append(it.next());
        }
        res.append(')');
        return res.toString();
    }
}
