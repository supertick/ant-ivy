/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.plugins.resolver;

import java.io.File;
import java.text.ParseException;
import java.util.List;

import junit.framework.TestCase;

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultIncludeRule;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.plugins.matcher.ExactPatternMatcher;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.MockMessageImpl;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;


/**
 * 
 */
public class IBiblioResolverTest extends TestCase {
	// remote.test

    private IvySettings _settings;
    private ResolveEngine _engine;
    private ResolveData _data;
    private File _cache;
    
    
    protected void setUp() throws Exception {
    	_settings = new IvySettings();
        _engine = new ResolveEngine(_settings, new EventManager(), new SortEngine(_settings));
        _cache = new File("build/cache");
        _data = new ResolveData(_engine, new ResolveOptions().setCache(CacheManager.getInstance(_settings, _cache)));
        _cache.mkdirs();
        _settings.setDefaultCache(_cache);
    }
    
    protected void tearDown() throws Exception {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(_cache);
        del.execute();
    }
    
    public void testDefaults() {
        IBiblioResolver resolver = new IBiblioResolver();
        _settings.setVariable("ivy.ibiblio.default.artifact.root", "http://www.ibiblio.org/mymaven/");
        _settings.setVariable("ivy.ibiblio.default.artifact.pattern", "[module]/jars/[artifact]-[revision].jar");
        resolver.setSettings(_settings);
        List l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/mymaven/[module]/jars/[artifact]-[revision].jar", l.get(0));
    }

    public void testInitFromConf() throws Exception {
        _settings.setVariable("ivy.ibiblio.default.artifact.root", "http://www.ibiblio.org/maven/");
        _settings.setVariable("ivy.ibiblio.default.artifact.pattern", "[module]/jars/[artifact]-[revision].jar");
        _settings.setVariable("my.ibiblio.root", "http://www.ibiblio.org/mymaven/");
        _settings.setVariable("my.ibiblio.pattern", "[module]/[artifact]-[revision].jar");
        _settings.load(IBiblioResolverTest.class.getResource("ibiblioresolverconf.xml"));
        IBiblioResolver resolver = (IBiblioResolver)_settings.getResolver("ibiblioA");
        assertNotNull(resolver);
        List l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/mymaven/[module]/[artifact]-[revision].jar", l.get(0));
        
        resolver = (IBiblioResolver)_settings.getResolver("ibiblioB");
        assertNotNull(resolver);
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/mymaven/[organisation]/jars/[artifact]-[revision].jar", l.get(0));

        resolver = (IBiblioResolver)_settings.getResolver("ibiblioC");
        assertTrue(resolver.isM2compatible());
        assertNotNull(resolver);
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/maven2/[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]", l.get(0));

        resolver = (IBiblioResolver)_settings.getResolver("ibiblioD");
        assertFalse(resolver.isM2compatible());
        assertNotNull(resolver);
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/maven/[module]/jars/[artifact]-[revision].jar", l.get(0));

        resolver = (IBiblioResolver)_settings.getResolver("ibiblioE");
        assertTrue(resolver.isM2compatible());
        assertNotNull(resolver);
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/mymaven/[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]", l.get(0));

        resolver = (IBiblioResolver)_settings.getResolver("ibiblioF");
        assertTrue(resolver.isM2compatible());
        assertNotNull(resolver);
        l = resolver.getArtifactPatterns();
        assertNotNull(l);
        assertEquals(1, l.size());
        assertEquals("http://www.ibiblio.org/mymaven/[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]", l.get(0));
    }

    public void testIBiblio() throws Exception {
        String ibiblioRoot = IBiblioHelper.getIBiblioMirror();
        if (ibiblioRoot == null) {
            return;
        }
        
        IBiblioResolver resolver = new IBiblioResolver();
        resolver.setRoot(ibiblioRoot);
        resolver.setName("test");
        resolver.setSettings(_settings);
        assertEquals("test", resolver.getName());
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("apache", "commons-fileupload", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), _data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());

        DefaultArtifact artifact = new DefaultArtifact(mrid, rmr.getPublicationDate(), "commons-fileupload", "jar", "jar");
        DownloadReport report = resolver.download(new Artifact[] {artifact}, new DownloadOptions(_settings, _cache));
        assertNotNull(report);
        
        assertEquals(1, report.getArtifactsReports().length);
        
        ArtifactDownloadReport ar = report.getArtifactReport(artifact);
        assertNotNull(ar);
        
        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());

        // test to ask to download again, should use cache
        report = resolver.download(new Artifact[] {artifact}, new DownloadOptions(_settings, _cache));
        assertNotNull(report);
        
        assertEquals(1, report.getArtifactsReports().length);
        
        ar = report.getArtifactReport(artifact);
        assertNotNull(ar);
        
        assertEquals(artifact, ar.getArtifact());
        assertEquals(DownloadStatus.NO, ar.getDownloadStatus());
    }
    
    public void testErrorReport() throws Exception {
        IBiblioResolver resolver = new IBiblioResolver();
        resolver.setRoot("http://unknown.host.comx/");
        resolver.setName("test");
        resolver.setM2compatible(true);
        resolver.setSettings(_settings);
        assertEquals("test", resolver.getName());
        
        MockMessageImpl mockMessageImpl = new MockMessageImpl();
		Message.setImpl(mockMessageImpl);
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("org.apache", "commons-fileupload", "1.0");
        ResolvedModuleRevision rmr = resolver.getDependency(new DefaultDependencyDescriptor(mrid, false), _data);
        assertNull(rmr);
    	
        mockMessageImpl.assertLogContains("tried http://unknown.host.comx/org/apache/commons-fileupload/1.0/commons-fileupload-1.0.pom");
        mockMessageImpl.assertLogContains("tried http://unknown.host.comx/org/apache/commons-fileupload/1.0/commons-fileupload-1.0.jar");
    }

    public void testIBiblioArtifacts() throws Exception {
        String ibiblioRoot = IBiblioHelper.getIBiblioMirror();
        if (ibiblioRoot == null) {
            return;
        }
        
        IBiblioResolver resolver = new IBiblioResolver();
        resolver.setRoot(ibiblioRoot);
        resolver.setName("test");
        resolver.setSettings(_settings);
        assertEquals("test", resolver.getName());
        
        ModuleRevisionId mrid = ModuleRevisionId.newInstance("apache", "nanning", "0.9");
        DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(mrid, false);
        dd.addIncludeRule("default", new DefaultIncludeRule(new ArtifactId(mrid.getModuleId(), "nanning-profiler", "jar", "jar"), ExactPatternMatcher.INSTANCE, null));
        dd.addIncludeRule("default", new DefaultIncludeRule(new ArtifactId(mrid.getModuleId(), "nanning-trace", "jar", "jar"), ExactPatternMatcher.INSTANCE, null));
        ResolvedModuleRevision rmr = resolver.getDependency(dd, _data);
        assertNotNull(rmr);
        assertEquals(mrid, rmr.getId());

        DefaultArtifact profiler = new DefaultArtifact(mrid, rmr.getPublicationDate(), "nanning-profiler", "jar", "jar");
        DefaultArtifact trace = new DefaultArtifact(mrid, rmr.getPublicationDate(), "nanning-trace", "jar", "jar");
        DownloadReport report = resolver.download(new Artifact[] {profiler, trace}, new DownloadOptions(_settings, _cache));
        assertNotNull(report);
        
        assertEquals(2, report.getArtifactsReports().length);
        
        ArtifactDownloadReport ar = report.getArtifactReport(profiler);
        assertNotNull(ar);
        
        assertEquals(profiler, ar.getArtifact());
        assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());

        ar = report.getArtifactReport(trace);
        assertNotNull(ar);
        
        assertEquals(trace, ar.getArtifact());
        assertEquals(DownloadStatus.SUCCESSFUL, ar.getDownloadStatus());

        // test to ask to download again, should use cache
        report = resolver.download(new Artifact[] {profiler, trace}, new DownloadOptions(_settings, _cache));
        assertNotNull(report);
        
        assertEquals(2, report.getArtifactsReports().length);
        
        ar = report.getArtifactReport(profiler);
        assertNotNull(ar);
        
        assertEquals(profiler, ar.getArtifact());
        assertEquals(DownloadStatus.NO, ar.getDownloadStatus());

        ar = report.getArtifactReport(trace);
        assertNotNull(ar);
        
        assertEquals(trace, ar.getArtifact());
        assertEquals(DownloadStatus.NO, ar.getDownloadStatus());
    }

    public void testUnknown() throws Exception {
        String ibiblioRoot = IBiblioHelper.getIBiblioMirror();
        if (ibiblioRoot == null) {
            return;
        }
        
        IBiblioResolver resolver = new IBiblioResolver();
        resolver.setRoot(ibiblioRoot);
        resolver.setName("test");
        resolver.setSettings(_settings);

        assertNull(resolver.getDependency(new DefaultDependencyDescriptor(ModuleRevisionId.newInstance("unknown", "unknown", "1.0"), false), _data));
    }

}
