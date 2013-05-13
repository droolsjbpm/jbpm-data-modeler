/**
 * Copyright 2012 JBoss Inc
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

package org.jbpm.datamodeler.driver.impl.annotations;

import org.jbpm.datamodeler.core.impl.AbstractAnnotationDefinition;
import org.jbpm.datamodeler.core.impl.AnnotationMemberDefinitionImpl;

public class PositionAnnotationDefinition extends AbstractAnnotationDefinition {

    public PositionAnnotationDefinition() {

        super("@Position", org.kie.api.definition.type.Position.class.getName(), "Position", "Position annotation", false, true);
        addMember(new AnnotationMemberDefinitionImpl("value", Integer.class.getName(), false, "", "value", "value"));
    }

    public static PositionAnnotationDefinition getInstance() {
        return new PositionAnnotationDefinition();
    }
}
