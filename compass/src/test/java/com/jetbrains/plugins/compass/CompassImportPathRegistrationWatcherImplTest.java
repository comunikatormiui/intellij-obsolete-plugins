package com.jetbrains.plugins.compass;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.OrderEntryUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.plugins.sass.extensions.SassRubyIntegrationHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CompassImportPathRegistrationWatcherImplTest extends CompassTestCase {
  private VirtualFile myConfigFile;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    myConfigFile = VfsUtil.findFileByIoFile(FileUtil.createTempFile("compassConfig", ".txt"), true);
    assertNotNull(myConfigFile);
    myCompassSettings.setCompassSupportEnabled(true);
    myCompassSettings.setCompassConfigPath(myConfigFile.getCanonicalPath());
    CompassUtil.getCompassExtension().stopActivity(myFixture.getModule());
    CompassUtil.getCompassExtension().startActivity(myFixture.getModule());
    UIUtil.dispatchAllInvocationEvents();
  }

  @Override
  public void tearDown() throws Exception {
    try {
      deleteConfigFile();
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  public void testAddLibraryForEachAddImportPathCall() throws Exception {
    final VirtualFile importPath1 = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath1", ""), true);
    final VirtualFile importPath2 = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath2", ""), true);
    assertNotNull(importPath1);
    assertNotNull(importPath2);

    assertSettingsAndLibraryRoots();
    updateConfigFile("add_import_path '" + importPath1.getPath() + "'\n" +
                     "add_import_path '" + importPath2.getPath() + "'\n");
    assertSettingsAndLibraryRoots(importPath1, importPath2);
  }

  public void testRemoveImportPathsAfterConfigRemove() throws Exception {
    final VirtualFile importPath = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath", ""), true);
    assertNotNull(importPath);

    assertSettingsAndLibraryRoots();
    updateConfigFile("add_import_path '" + importPath.getPath() + "'");
    assertSettingsAndLibraryRoots(importPath);

    deleteConfigFile();
    assertSettingsAndLibraryRoots();
  }

  public void testRenameConfig() throws Exception {
    String oldName = myConfigFile.getName();

    final VirtualFile importPath = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath", ""), true);
    assertNotNull(importPath);

    assertSettingsAndLibraryRoots();
    updateConfigFile("add_import_path '" + importPath.getPath() + "'");
    assertSettingsAndLibraryRoots(importPath);

    renameConfigFile("config_inappropriate_name.rb");
    assertSettingsAndLibraryRoots();

    renameConfigFile(oldName);
    assertSettingsAndLibraryRoots(importPath);
  }

  public void testAddLibraryForLastAdditionalImportPathsAssignment() throws Exception {
    final VirtualFile importPath1 = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath1", ""), true);
    final VirtualFile importPath2 = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath2", ""), true);
    assertNotNull(importPath1);
    assertNotNull(importPath2);

    assertSettingsAndLibraryRoots();
    updateConfigFile("additional_import_paths = ['" + importPath1.getPath() + "']\n" +
                     "additional_import_paths = ['" + importPath2.getPath() + "']\n");
    assertSettingsAndLibraryRoots(importPath2);
  }

  public void testCombined() throws Exception {
    final VirtualFile importPath1 = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath1", ""), true);
    final VirtualFile importPath2 = VfsUtil.findFileByIoFile(FileUtil.createTempDirectory("importPath2", ""), true);
    assertNotNull(importPath1);
    assertNotNull(importPath2);

    assertSettingsAndLibraryRoots();
    updateConfigFile("additional_import_paths = ['" + importPath1.getPath() + "']\n" +
                     "add_import_path '" + importPath2.getPath() + "'\n");
    assertSettingsAndLibraryRoots(importPath1, importPath2);
  }

  private void assertSettingsAndLibraryRoots(VirtualFile... expectedLibraryRoots) {
    UIUtil.dispatchAllInvocationEvents();
    final String[] expectedImportPaths = ArrayUtil.prepend(getCompassStylesheetsRoot().getPath(),
                                                     ContainerUtil.map(expectedLibraryRoots, file -> file.getPath(),
                                                                       ArrayUtilRt.EMPTY_STRING_ARRAY));
    assertSameElements(CompassSettings.getInstance(myFixture.getModule()).getImportPaths(), expectedImportPaths);

    final ModifiableModelsProvider modelsProvider = ModifiableModelsProvider.SERVICE.getInstance();
    final ModifiableRootModel model = modelsProvider.getModuleModifiableModel(myFixture.getModule());
    try {
      final LibraryOrderEntry compassLibraryEntry = OrderEntryUtil.findLibraryOrderEntry(model, CompassUtil.COMPASS_LIBRARY_NAME);
      if (compassLibraryEntry == null) {
        assertEquals("compass library not found", 0, expectedLibraryRoots.length);
      }
      else {
        final List<VirtualFile> actualRoots = Arrays.asList(compassLibraryEntry.getRootFiles(OrderRootType.CLASSES));
        assertSameElements(actualRoots, ArrayUtil.prepend(getCompassRoot(), expectedLibraryRoots));
      }
    }
    finally {
      modelsProvider.disposeModuleModifiableModel(model);
    }
  }

  private void renameConfigFile(final String newName) {
    WriteCommandAction.runWriteCommandAction(null, new Runnable() {
      @Override
      public void run() {
        try {
          myConfigFile.rename(this, newName);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  private void deleteConfigFile() {
    WriteCommandAction.runWriteCommandAction(myFixture.getProject(), () -> {
      try {
        if (myConfigFile != null) {
          myConfigFile.delete(null);
          myConfigFile = null;
        }
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void updateConfigFile(String newText) {
    myFixture.saveText(myConfigFile, newText);
    PsiDocumentManager.getInstance(myFixture.getProject()).commitAllDocuments();
  }
}
