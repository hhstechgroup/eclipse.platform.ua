/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ua.tests.help.other;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.help.IIndexSubpath;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.index.IndexSee;
import org.eclipse.ua.tests.help.util.DocumentCreator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class IndexSeeTest extends TestCase {

	private static final String AGUILA = "\u00E1guila"; // 00E1 is an accented letter 'a'
	private static final String ECLIPSE = "eclipse";
	private static final String SDK = "sdk";
	private static final String VIEWS = "views";
    private static final String SEE_END = "</see>";
	private static final String SEE_HEAD_ECLIPSE = "<see keyword=\"eclipse\">";
	private static final String SEE_ECLIPSE = "<see keyword=\"eclipse\"/>";
	private static final String SEE_SDK = "<see keyword=\"sdk\"/>";
	private static final String SUBPATH_SDK = "<subpath keyword=\"sdk\">";
	private static final String SUBPATH_VIEWS = "<subpath keyword=\"views\">";
	private static final String SUBPATH_END = "</subpath>";
	private static final String SEE_ECLIPSE_SDK = SEE_HEAD_ECLIPSE + 
    SUBPATH_SDK + SUBPATH_END + SEE_END;
	private static final String SEE_ECLIPSE_VIEWS = SEE_HEAD_ECLIPSE + 
    SUBPATH_SDK + SUBPATH_END + SUBPATH_VIEWS + SUBPATH_END + SEE_END;
	private static final String SEE_ECLIPSE_SDK_VIEWS = SEE_HEAD_ECLIPSE + 
    SUBPATH_SDK + SUBPATH_END + SUBPATH_VIEWS + SUBPATH_END + SEE_END;
	
	public static Test suite() {
		return new TestSuite(IndexSeeTest.class);
	}
	
	protected void setUp() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_WORKBENCH);
	}

	private IndexSee createSee(final String elementSource) {
		IndexSee element;
		Document doc;
		try {
		    doc = DocumentCreator.createDocument(elementSource);
		} catch (Exception e) {
			fail("Caught Exception");
			doc = null;
		}
		element = new IndexSee((Element) doc.getFirstChild());
		return element;
	}
	
	private IndexSee createSimpleSee(final String keyword) {
		IndexSee element;
		Document doc;
		String elementSource = "<see keyword=\"" + keyword + "\" />";
		try {
		    doc = DocumentCreator.createDocument(elementSource);
		} catch (Exception e) {
			fail("Caught Exception");
			doc = null;
		}
		element = new IndexSee((Element) doc.getFirstChild());
		return element;
	}
	

	public void testSimpleIndexSee() {
		IndexSee see;
		see = createSee(SEE_ECLIPSE);
		assertEquals(ECLIPSE, see.getKeyword());

	}

	public void testCopySimpleIndexSee() {
		IndexSee see1;
		see1 = createSee(SEE_ECLIPSE);
		IndexSee see2 = new IndexSee(see1);
		assertEquals(ECLIPSE, see1.getKeyword());
        assertEquals(0, see1.getSubpathElements().length);
		assertEquals(ECLIPSE, see1.getKeyword());
		
		assertEquals(ECLIPSE, see2.getKeyword());
        assertEquals(0, see2.getSubpathElements().length);
		assertEquals(ECLIPSE, see2.getKeyword());
	}

	public void testCopyIndexSeeWithSubpath() {
		IndexSee see1;
		see1 = createSee(SEE_ECLIPSE_SDK);
		IndexSee see2 = new IndexSee(see1);

		assertEquals(1, see1.getSubpathElements().length);
		assertEquals(ECLIPSE, see1.getKeyword());
		assertEquals(SDK, see1.getSubpathElements()[0].getKeyword());
		
		assertEquals(1, see2.getSubpathElements().length);
		assertEquals(ECLIPSE, see2.getKeyword());
		assertEquals(SDK, see2.getSubpathElements()[0].getKeyword());
		
	}
	
	public void testCopyIndexSeeWithLongerSubpath() {
		IndexSee see1;
		see1 = createSee(SEE_ECLIPSE_SDK_VIEWS);
		IndexSee see2 = new IndexSee(see1);

		assertEquals(2, see1.getSubpathElements().length);
		assertEquals(ECLIPSE, see1.getKeyword());
		assertEquals(SDK, see1.getSubpathElements()[0].getKeyword());
		assertEquals(VIEWS, see1.getSubpathElements()[1].getKeyword());
		
		assertEquals(2, see2.getSubpathElements().length);
		assertEquals(ECLIPSE, see2.getKeyword());
		assertEquals(SDK, see2.getSubpathElements()[0].getKeyword());
		assertEquals(VIEWS, see2.getSubpathElements()[1].getKeyword());	
	}

	public void testCompareSimpleSame() {
		IndexSee see1 = createSee(SEE_ECLIPSE);
		IndexSee see2 = createSee(SEE_ECLIPSE);
		assertTrue (see1.equals(see2));
		assertEquals(0, see1.compareTo(see2));
		assertEquals(0, see2.compareTo(see1));
		assertEquals(see1.hashCode(), see2.hashCode());
	}

	public void testCompareSimpleDifferent() {
		IndexSee see1 = createSee(SEE_ECLIPSE);
		IndexSee see2 = createSee(SEE_SDK);
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) < 0);
		assertTrue(see2.compareTo(see1) > 0);
	}

	public void testCompareCompoundSame() {
		IndexSee see1 = createSee(SEE_ECLIPSE_SDK);
		IndexSee see2 = createSee(SEE_ECLIPSE_SDK);
		assertTrue (see1.equals(see2));
		assertEquals(0, see1.compareTo(see2));
		assertEquals(0, see2.compareTo(see1));
		assertEquals(see1.hashCode(), see2.hashCode());
	}

	public void testCompareCompoundDifferent() {
		IndexSee see1 = createSee(SEE_ECLIPSE_SDK);
		IndexSee see2 = createSee(SEE_ECLIPSE_VIEWS);
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) < 0);
		assertTrue(see2.compareTo(see1) > 0);
	}
	
	public void testCompareCompoundDifferentLengths() {
		IndexSee see1 = createSee(SEE_ECLIPSE_SDK);
		IndexSee see2 = createSee(SEE_ECLIPSE_SDK_VIEWS);
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) < 0);
		assertTrue(see2.compareTo(see1) > 0);
	}

	public void testCompare_AAA_abacus() {
		IndexSee see1 = createSimpleSee("AAA");
		IndexSee see2 = createSimpleSee("abacus");
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) < 0);
		assertTrue(see2.compareTo(see1) > 0);
	}

	public void testCompare_abacus_ABC() {
		IndexSee see1 = createSimpleSee("abacus");
		IndexSee see2 = createSimpleSee(AGUILA);
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) < 0);
		assertTrue(see2.compareTo(see1) > 0);
	}

	public void testCompare_ABC_aguila() {
		IndexSee see1 = createSimpleSee("ABC");
		IndexSee see2 = createSimpleSee(AGUILA);
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) < 0);
		assertTrue(see2.compareTo(see1) > 0);
	}

	public void testCompare_aguila_axe() {
		IndexSee see1 = createSimpleSee(AGUILA);
		IndexSee see2 = createSimpleSee("axe");
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) < 0);
		assertTrue(see2.compareTo(see1) > 0);
	}
	
	public void testCompare_to_underscore() {
		IndexSee see1 = createSimpleSee("abc");
		IndexSee see2 = createSimpleSee("_xyz");
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) > 0);
		assertTrue(see2.compareTo(see1) < 0);
	}

	public void testUserSee() {
	     UserIndexSee u1;
	     u1 = createUserSee();
	     IndexSee see = new IndexSee(u1);
	     checkCreatedSee(see);
	}

	public void testCopyUserSee() {
	     UserIndexSee u1;
	     u1 = createUserSee();
	     IndexSee see = new IndexSee(u1);
	     IndexSee see2 = new IndexSee(see);
	     checkCreatedSee(see);
	     checkCreatedSee(see2);
	}
	
	public void testCreateTwiceUserSee() {
	     UserIndexSee u1;
	     u1 = createUserSee();
	     IndexSee see = new IndexSee(u1);
	     IndexSee see2 = new IndexSee(u1);
	     checkCreatedSee(see);
	     checkCreatedSee(see2);
	}

	private UserIndexSee createUserSee() {
		UserIndexSee u1;
		u1 = new UserIndexSee("eclipse", false);
	     UserIndexSubpath u2 = new UserIndexSubpath("platform");
	     UserIndexSubpath u3 = new UserIndexSubpath("ui");
	     u1.addSubpath(u2);
	     u1.addSubpath(u3);
		return u1;
	}

	private void checkCreatedSee(IndexSee see) {
		assertEquals("eclipse", see.getKeyword());
	     IIndexSubpath[] subpath = see.getSubpathElements();
	     assertEquals(2, subpath.length);
	     assertEquals("platform", subpath[0].getKeyword());
	     assertEquals("ui", subpath[1].getKeyword());
	}

}