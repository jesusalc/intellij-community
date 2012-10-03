/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ide.util.newProjectWizard;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.util.treeView.AlphaComparator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.platform.ProjectTemplate;
import com.intellij.platform.ProjectTemplatesFactory;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.speedSearch.ElementFilter;
import com.intellij.ui.treeStructure.*;
import com.intellij.ui.treeStructure.filtered.FilteringTreeBuilder;
import com.intellij.ui.treeStructure.filtered.FilteringTreeStructure;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.text.Matcher;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Dmitry Avdeev
 *         Date: 9/26/12
 */
public class SelectTemplateStep extends ModuleWizardStep {

  private JPanel myPanel;
  private SimpleTree myTemplatesTree;
  private JPanel mySettingsPanel;
  private SearchTextField mySearchField;
  private JTextPane myDescriptionPane;
  private JPanel myDescriptionPanel;

  private final WizardContext myContext;
  private final ElementFilter.Active.Impl<SimpleNode> myFilter;
  private final FilteringTreeBuilder myBuilder;
  private MinusculeMatcher[] myMatchers;

  public SelectTemplateStep(WizardContext context) {

    myContext = context;
    Messages.installHyperlinkSupport(myDescriptionPane);

    ProjectTemplatesFactory[] factories = ProjectTemplatesFactory.EP_NAME.getExtensions();
    final MultiMap<String, ProjectTemplatesFactory> groups = new MultiMap<String, ProjectTemplatesFactory>();
    for (ProjectTemplatesFactory factory : factories) {
      for (String string : factory.getGroups()) {
        groups.putValue(string, factory);
      }
    }

    SimpleTreeStructure.Impl structure = new SimpleTreeStructure.Impl(new SimpleNode() {
      @Override
      public SimpleNode[] getChildren() {
        return ContainerUtil.map2Array(groups.entrySet(), NO_CHILDREN, new Function<Map.Entry<String, Collection<ProjectTemplatesFactory>>, SimpleNode>() {
          @Override
          public SimpleNode fun(Map.Entry<String, Collection<ProjectTemplatesFactory>> entry) {
            return new GroupNode(entry.getKey(), entry.getValue());
          }
        });
      }
    });

    buildMatcher();
    myFilter = new ElementFilter.Active.Impl<SimpleNode>() {
      @Override
      public boolean shouldBeShowing(SimpleNode template) {
        return matches(template);
      }
    };
    myBuilder = new FilteringTreeBuilder(myTemplatesTree, myFilter, structure, AlphaComparator.INSTANCE);

    myTemplatesTree.setRootVisible(false);
    myTemplatesTree.setShowsRootHandles(false);
    myTemplatesTree.setCellRenderer(new ColoredTreeCellRenderer() {
      @Override
      public void customizeCellRenderer(JTree tree,
                                        Object value,
                                        boolean selected,
                                        boolean expanded,
                                        boolean leaf,
                                        int row,
                                        boolean hasFocus) {
//        setIpad(new Insets());
        SimpleNode node = getSimpleNode(value);
        if (node != null) {
          String name = node.getName();
          if (name != null) {
            append(name);
          }
        }
        if (node instanceof GroupNode) {
          setIcon(UIUtil.getTreeIcon(expanded));
        }
      }
    });

    myTemplatesTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        if (mySettingsPanel.getComponentCount() > 0) {
          mySettingsPanel.remove(0);
        }
        ProjectTemplate template = getSelectedTemplate();
        if (template != null) {
          JComponent settingsPanel = template.getSettingsPanel();
          if (settingsPanel != null) {
            mySettingsPanel.add(settingsPanel, BorderLayout.NORTH);
          }
          mySettingsPanel.setVisible(settingsPanel != null);
          String description = template.getDescription();
          myDescriptionPane.setText(description);
          myDescriptionPanel.setVisible(StringUtil.isNotEmpty(description));
        }
        else {
          mySettingsPanel.setVisible(false);
          myDescriptionPanel.setVisible(false);
        }
        mySettingsPanel.revalidate();
        mySettingsPanel.repaint();
      }
    });

    //if (myTemplatesTree.getModel().getSize() > 0) {
    //  myTemplatesTree.setSelectedIndex(0);
    //}
    mySearchField.addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        doFilter();
      }
    });
//    doFilter();
  }

  private void doFilter() {
    buildMatcher();
    SimpleNode selectedNode = getSelectedNode();
    final Ref<SimpleNode> node = new Ref<SimpleNode>();
    if (!(selectedNode instanceof TemplateNode) || !matches(selectedNode)) {
      myTemplatesTree.accept(myBuilder, new SimpleNodeVisitor() {
        @Override
        public boolean accept(SimpleNode simpleNode) {
          FilteringTreeStructure.FilteringNode wrapper = (FilteringTreeStructure.FilteringNode)simpleNode;
          Object delegate = wrapper.getDelegate();
          if (delegate instanceof TemplateNode && matches((SimpleNode)delegate)) {
            node.set((SimpleNode)delegate);
            return true;
          }
          return false;
        }
      });
    }

    myFilter.fireUpdate(node.get(), true, false);
  }

  private boolean matches(SimpleNode template) {
    String name = template.getName();
    if (name == null) return false;
    String[] words = NameUtil.nameToWords(name);
    for (String word : words) {
      for (Matcher matcher : myMatchers) {
        if (matcher.matches(word)) return true;
      }
    }
    return false;
  }

  private void buildMatcher() {
    String text = mySearchField.getText();
    myMatchers = ContainerUtil.map2Array(text.split(" "), MinusculeMatcher.class, new Function<String, MinusculeMatcher>() {
      @Override
      public MinusculeMatcher fun(String s) {
        return NameUtil.buildMatcher(s, NameUtil.MatchingCaseSensitivity.NONE);
      }
    });
  }

  @Nullable
  public ProjectTemplate getSelectedTemplate() {
    SimpleNode delegate = getSelectedNode();
    return delegate instanceof TemplateNode ? ((TemplateNode)delegate).myTemplate : null;
  }

  @Nullable
  private SimpleNode getSelectedNode() {
    TreePath path = myTemplatesTree.getSelectionPath();
    if (path == null) return null;
    return getSimpleNode(path.getLastPathComponent());
  }

  @Nullable
  private SimpleNode getSimpleNode(Object component) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)component;
    Object userObject = node.getUserObject();
    if (!(userObject instanceof FilteringTreeStructure.FilteringNode)) return null;
    FilteringTreeStructure.FilteringNode object = (FilteringTreeStructure.FilteringNode)userObject;
    return (SimpleNode)object.getDelegate();
  }

  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return mySearchField;
  }

  @Override
  public void updateDataModel() {
  }

  @Override
  public void disposeUIResources() {
    Disposer.dispose(myBuilder);
  }

  private class GroupNode extends SimpleNode {
    private final String myGroup;
    private final Collection<ProjectTemplatesFactory> myFactories;

    public GroupNode(String group, Collection<ProjectTemplatesFactory> factories) {
      myGroup = group;
      myFactories = factories;
    }

    @Override
    public SimpleNode[] getChildren() {
      List<SimpleNode> children = new ArrayList<SimpleNode>();
      for (ProjectTemplatesFactory factory : myFactories) {
        ProjectTemplate[] templates = factory.createTemplates(myGroup, myContext);
        for (ProjectTemplate template : templates) {
          children.add(new TemplateNode(template));
        }
      }
      return children.toArray(new SimpleNode[children.size()]);
    }


    @Override
    public String getName() {
      return myGroup;
    }
  }

  private static class TemplateNode extends NullNode {

    private final ProjectTemplate myTemplate;

    public TemplateNode(ProjectTemplate template) {
      myTemplate = template;
    }

    @Override
    public String getName() {
      return myTemplate.getName();
    }
  }
}