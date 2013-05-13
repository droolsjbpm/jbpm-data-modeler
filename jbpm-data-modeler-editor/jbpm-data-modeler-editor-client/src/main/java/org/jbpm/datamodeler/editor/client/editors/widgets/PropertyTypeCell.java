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

package org.jbpm.datamodeler.editor.client.editors.widgets;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import org.jbpm.datamodeler.editor.events.DataModelerEvent;
import org.jbpm.datamodeler.editor.model.ObjectPropertyTO;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import static com.google.gwt.dom.client.BrowserEvents.CLICK;

public class PropertyTypeCell extends TextCell {

    private boolean navigable = false;
    
    DataObjectBrowser editor;

    @Inject
    private javax.enterprise.event.Event<DataModelerEvent> dataModelerEvent;

    public PropertyTypeCell(boolean navigable, DataObjectBrowser editor) {
        super();        
        this.navigable = navigable;
        this.editor = editor;
    }

    @Override
    public Set<String> getConsumedEvents() {
        Set<String> consumedEvents = new HashSet<String>();
        consumedEvents.add(CLICK);
        return consumedEvents;
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, String value, NativeEvent event, ValueUpdater<String> stringValueUpdater) {

        ObjectPropertyTO property = (ObjectPropertyTO)context.getKey();
        if (DOM.eventGetType((Event) event) == Event.ONCLICK && !property.isBaseType()) {
            editor.onTypeCellSelection(property);
        } else {
           super.onBrowserEvent(context, parent, value, event, stringValueUpdater);
        }
    }

    @Override
    public void render(Context context, SafeHtml value, SafeHtmlBuilder sb) {

        ObjectPropertyTO property = (ObjectPropertyTO)context.getKey();
        if (navigable && property != null && !property.isBaseType()) {
            SafeHtml startAnchor = null;
            SafeHtml endAnchor = null;
            startAnchor = new SafeHtml() {
                @Override
                public String asString() {
                    return "<div style=\"cursor: pointer;\">";
                }
            };

            endAnchor = new SafeHtml() {
                @Override
                public String asString() {
                    return "</div>";
                }
            };

            sb.append(startAnchor);
            sb.append(value);
            sb.append(endAnchor);

        } else {
            super.render(context, value, sb);
        }
    }

}
