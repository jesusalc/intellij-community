/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.fileEditor;

import com.intellij.openapi.components.ExpandMacroToPathMap;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.intellij.ui.docking.DockManager;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.jps.model.serialization.PathMacroUtil;

import java.io.File;

/**
 * @author Dmitry Avdeev
 *         Date: 4/16/13
 */
public class FileEditorManagerTest extends LightPlatformCodeInsightFixtureTestCase {

  private FileEditorManagerImpl myManager;

  public FileEditorManagerTest() {
    PlatformTestCase.initPlatformLangPrefix();
  }

  public void testManager() throws Exception {

    Document document = JDOMUtil.loadDocument("<component name=\"FileEditorManager\">\n" +
                                              "    <splitter split-orientation=\"horizontal\" split-proportion=\"0.43304527\">\n" +
                                              "      <split-first>\n" +
                                              "        <leaf>\n" +
                                              "          <file leaf-file-name=\"1.txt\" pinned=\"false\" current=\"false\" current-in-tab=\"false\">\n" +
                                              "            <entry file=\"file://$PROJECT_DIR$/src/1.txt\">\n" +
                                              "              <provider selected=\"true\" editor-type-id=\"text-editor\">\n" +
                                              "                <state line=\"0\" column=\"0\" selection-start=\"0\" selection-end=\"0\" vertical-scroll-proportion=\"0.0\">\n" +
                                              "                  <folding />\n" +
                                              "                </state>\n" +
                                              "              </provider>\n" +
                                              "            </entry>\n" +
                                              "          </file>\n" +
                                              "          <file leaf-file-name=\"2.txt\" pinned=\"false\" current=\"false\" current-in-tab=\"true\">\n" +
                                              "            <entry file=\"file://$PROJECT_DIR$/src/2.txt\">\n" +
                                              "              <provider selected=\"true\" editor-type-id=\"text-editor\">\n" +
                                              "                <state line=\"0\" column=\"0\" selection-start=\"0\" selection-end=\"0\" vertical-scroll-proportion=\"0.0\">\n" +
                                              "                  <folding />\n" +
                                              "                </state>\n" +
                                              "              </provider>\n" +
                                              "            </entry>\n" +
                                              "          </file>\n" +
                                              "        </leaf>\n" +
                                              "      </split-first>\n" +
                                              "      <split-second>\n" +
                                              "        <leaf>\n" +
                                              "          <file leaf-file-name=\"3.txt\" pinned=\"false\" current=\"true\" current-in-tab=\"true\">\n" +
                                              "            <entry file=\"file://$PROJECT_DIR$/src/3.txt\">\n" +
                                              "              <provider selected=\"true\" editor-type-id=\"text-editor\">\n" +
                                              "                <state line=\"0\" column=\"0\" selection-start=\"0\" selection-end=\"0\" vertical-scroll-proportion=\"0.0\">\n" +
                                              "                  <folding />\n" +
                                              "                </state>\n" +
                                              "              </provider>\n" +
                                              "            </entry>\n" +
                                              "          </file>\n" +
                                              "        </leaf>\n" +
                                              "      </split-second>\n" +
                                              "    </splitter>\n" +
                                              "  </component>");
    Element rootElement = document.getRootElement();
    ExpandMacroToPathMap map = new ExpandMacroToPathMap();
    map.addMacroExpand(PathMacroUtil.PROJECT_DIR_MACRO_NAME, getTestDataPath());
    map.substitute(rootElement, true, true);

    myManager.readExternal(rootElement);
    myManager.getMainSplitters().openFiles();
  }

  public void setUp() throws Exception {
    super.setUp();
    myManager = new FileEditorManagerImpl(getProject(), DockManager.getInstance(getProject()));
  }

  @Override
  protected void tearDown() throws Exception {
    myManager.closeAllFiles();
    super.tearDown();
  }

  @Override
  protected String getTestDataPath() {
    return PlatformTestUtil.getCommunityPath().replace(File.separatorChar, '/') + "/platform/platform-tests/testData/fileEditorManager";
  }
}
