/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal.cheatsheets.composite.parser;

import org.eclipse.ui.cheatsheets.ICompositeCheatSheetTask;
import org.eclipse.ui.cheatsheets.ITaskGroup;
import org.eclipse.ui.internal.cheatsheets.composite.model.AbstractTask;
import org.w3c.dom.Node;

public class TaskGroupParseStrategy implements ITaskParseStrategy {

	private String kind;

	public TaskGroupParseStrategy(String kind) {
		this.kind = kind;
	}
	
	public void init() {	
	}
	
	public boolean parseElementNode(Node childNode, Node parentNode,
			AbstractTask parentTask, IStatusContainer status) 
    {
		// Task children are handled by CompositeCheatSheetParser
		return false;
	}

	public void parsingComplete(AbstractTask parentTask, IStatusContainer status) {
		if (ITaskGroup.SEQUENCE.equals(kind)) {
			// Create dependencies between the children
			ICompositeCheatSheetTask[] children  = parentTask.getSubtasks();
			AbstractTask previous = null;
			AbstractTask next = null;
			for (int i = 0; i < children.length; i++) {
				previous = next;
				next = (AbstractTask)children[i];
				if (previous != null) {
					next.addRequiredTask(previous);
					previous.addSuccessorTask(next);
				}
			}
		}
	}


}