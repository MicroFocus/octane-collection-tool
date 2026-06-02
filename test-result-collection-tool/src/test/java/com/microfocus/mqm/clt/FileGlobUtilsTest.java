/*
 *     Copyright 2015-2023 Open Text
 *
 *     The only warranties for products and services of Open Text and
 *     its affiliates and licensors ("Open Text") are as may be set forth
 *     in the express warranty statements accompanying such products and services.
 *     Nothing herein should be construed as constituting an additional warranty.
 *     Open Text shall not be liable for technical or editorial errors or
 *     omissions contained herein. The information contained herein is subject
 *     to change without notice.
 *
 *     Except as specifically indicated otherwise, this document contains
 *     confidential information and a valid license is required for possession,
 *     use or copying. If this work is provided to the U.S. Government,
 *     consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *     Computer Software Documentation, and Technical Data for Commercial Items are
 *     licensed to the U.S. Government under vendor's standard commercial license.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.microfocus.mqm.clt;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileGlobUtilsTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void literal_existingFile_absolutePath_returnsIt() throws IOException {
        File f = tmp.newFile("report.xml");
        List<String> result = FileGlobUtils.resolveGlobPattern(f.getAbsolutePath());
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(f.getAbsolutePath(), result.get(0));
    }

    @Test
    public void literal_nonExistentFile_returnsEmpty() {
        List<String> result = FileGlobUtils.resolveGlobPattern("/this/path/does/not/exist/file.xml");
        Assert.assertTrue("Expected empty list for non-existent literal path", result.isEmpty());
    }

    @Test
    public void literal_directory_returnsEmpty() throws IOException {
        File dir = tmp.newFolder("notAFile");
        List<String> result = FileGlobUtils.resolveGlobPattern(dir.getAbsolutePath());
        Assert.assertTrue("Expected empty list when path points to a directory", result.isEmpty());
    }

    @Test
    public void singleLevel_matchesOnlyRequestedExtension() throws IOException {
        File dir = tmp.newFolder("reports");
        new File(dir, "a.xml").createNewFile();
        new File(dir, "b.xml").createNewFile();
        new File(dir, "c.txt").createNewFile();
        new File(dir, "d.json").createNewFile();

        String pattern = dir.getAbsolutePath() + File.separator + "*.xml";
        List<String> result = FileGlobUtils.resolveGlobPattern(pattern);

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.stream().anyMatch(p -> p.endsWith("a.xml")));
        Assert.assertTrue(result.stream().anyMatch(p -> p.endsWith("b.xml")));
        Assert.assertFalse(result.stream().anyMatch(p -> p.endsWith("c.txt")));
    }

    @Test
    public void singleLevel_noMatch_returnsEmpty() throws IOException {
        File dir = tmp.newFolder("emptyDir");
        String pattern = dir.getAbsolutePath() + File.separator + "*.xml";
        List<String> result = FileGlobUtils.resolveGlobPattern(pattern);
        Assert.assertTrue("Expected empty list when no files match pattern", result.isEmpty());
    }

    @Test
    public void singleLevel_nonExistentBaseDir_returnsEmpty() {
        String pattern = tmp.getRoot().getAbsolutePath() + File.separator + "nonexistent" + File.separator + "*.xml";
        List<String> result = FileGlobUtils.resolveGlobPattern(pattern);
        Assert.assertTrue("Expected empty list when base directory does not exist", result.isEmpty());
    }

    @Test
    public void singleLevel_doesNotDescendIntoSubdirectories() throws IOException {
        File dir = tmp.newFolder("parent");
        File sub = new File(dir, "child");
        sub.mkdir();
        new File(dir, "top.xml").createNewFile();
        new File(sub, "nested.xml").createNewFile();

        String pattern = dir.getAbsolutePath() + File.separator + "*.xml";
        List<String> result = FileGlobUtils.resolveGlobPattern(pattern);

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.get(0).endsWith("top.xml"));
        Assert.assertFalse(result.stream().anyMatch(p -> p.endsWith("nested.xml")));
    }

    @Test
    public void singleLevel_questionMarkWildcard() throws IOException {
        File dir = tmp.newFolder("qmarks");
        new File(dir, "t1.xml").createNewFile();
        new File(dir, "t2.xml").createNewFile();
        new File(dir, "t12.xml").createNewFile();

        String pattern = dir.getAbsolutePath() + File.separator + "t?.xml";
        List<String> result = FileGlobUtils.resolveGlobPattern(pattern);

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.stream().anyMatch(p -> p.endsWith("t1.xml")));
        Assert.assertTrue(result.stream().anyMatch(p -> p.endsWith("t2.xml")));
        Assert.assertFalse(result.stream().anyMatch(p -> p.endsWith("t12.xml")));
    }

    @Test
    public void singleLevel_starMatchesAllFiles() throws IOException {
        File dir = tmp.newFolder("allFiles");
        new File(dir, "foo.xml").createNewFile();
        new File(dir, "bar.txt").createNewFile();
        new File(dir, "baz.json").createNewFile();

        String pattern = dir.getAbsolutePath() + File.separator + "*";
        List<String> result = FileGlobUtils.resolveGlobPattern(pattern);

        Assert.assertEquals(3, result.size());
    }

    @Test
    public void singleLevel_resultsAreAbsolutePaths() throws IOException {
        File dir = tmp.newFolder("absPaths");
        new File(dir, "report.xml").createNewFile();

        String pattern = dir.getAbsolutePath() + File.separator + "*.xml";
        List<String> result = FileGlobUtils.resolveGlobPattern(pattern);

        Assert.assertEquals(1, result.size());
        Assert.assertTrue("Path should be absolute", new File(result.get(0)).isAbsolute());
    }

    @Test
    public void recursive_matchesFilesAtAllDepths() throws IOException {
        File root = tmp.newFolder("root");
        File sub = new File(root, "sub");
        File deep = new File(sub, "deep");
        sub.mkdirs();
        deep.mkdirs();

        new File(root, "top.xml").createNewFile();
        new File(sub, "mid.xml").createNewFile();
        new File(deep, "bottom.xml").createNewFile();
        new File(deep, "other.txt").createNewFile();

        String pattern = root.getAbsolutePath() + File.separator + "**" + File.separator + "*.xml";
        List<String> result = FileGlobUtils.resolveGlobPattern(pattern);

        Assert.assertEquals(3, result.size());
        Assert.assertTrue(result.stream().anyMatch(p -> p.endsWith("top.xml")));
        Assert.assertTrue(result.stream().anyMatch(p -> p.endsWith("mid.xml")));
        Assert.assertTrue(result.stream().anyMatch(p -> p.endsWith("bottom.xml")));
        Assert.assertFalse(result.stream().anyMatch(p -> p.endsWith("other.txt")));
    }

    @Test
    public void recursive_noMatch_returnsEmpty() throws IOException {
        File root = tmp.newFolder("emptyTree");
        new File(root, "sub").mkdirs();

        String pattern = root.getAbsolutePath() + File.separator + "**" + File.separator + "*.xml";
        List<String> result = FileGlobUtils.resolveGlobPattern(pattern);

        Assert.assertTrue("Expected empty list when no files match in recursive walk", result.isEmpty());
    }

    @Test
    public void recursive_onlyFilesReturned_notDirectories() throws IOException {
        File root = tmp.newFolder("dirCheck");
        new File(root, "sub").mkdirs();
        new File(root, "file.xml").createNewFile();

        String pattern = root.getAbsolutePath() + File.separator + "**" + File.separator + "*";
        List<String> result = FileGlobUtils.resolveGlobPattern(pattern);

        for (String path : result) {
            Assert.assertTrue("Only regular files should be returned: " + path, new File(path).isFile());
        }
    }

    @Test
    public void forwardSlashSeparator_treatedSameAsSystemSeparator() throws IOException {
        File dir = tmp.newFolder("slashTest");
        new File(dir, "result.xml").createNewFile();

        // Use forward slash even on Windows
        String pattern = dir.getAbsolutePath().replace('\\', '/') + "/*.xml";
        List<String> result = FileGlobUtils.resolveGlobPattern(pattern);

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.get(0).endsWith("result.xml"));
    }

    @Test
    public void singleLevel_matchesSampleJacocoFiles() {
        String pattern = "src/test/resources/com/microfocus/mqm/clt/sample-*.xml";
        List<String> result = FileGlobUtils.resolveGlobPattern(pattern);

        Assert.assertEquals("Expected exactly 2 sample jacoco files", 2, result.size());
        Assert.assertTrue(result.stream().anyMatch(p -> p.endsWith("sample-jacoco.xml")));
        Assert.assertTrue(result.stream().anyMatch(p -> p.endsWith("sample-jacoco-2.xml")));
    }
}