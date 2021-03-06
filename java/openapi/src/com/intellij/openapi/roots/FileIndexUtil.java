/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.roots;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.fileTypes.FileTypeManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class FileIndexUtil {
  private FileIndexUtil() {
  }

  public static boolean isJavaSourceFile(@NotNull Project project, @NotNull VirtualFile file) {
    FileTypeManager fileTypeManager = FileTypeManager.getInstance();
    if (file.isDirectory()) return false;
    if (file.getFileType() != StdFileTypes.JAVA) return false;
    if (fileTypeManager.isFileIgnored(file)) return false;
    return ProjectRootManager.getInstance(project).getFileIndex().isInSource(file);
  }
}
