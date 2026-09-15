/*
 * The MIT License
 *
 * Copyright 2026 zzambers.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package resultsview.xml;

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import resultsview.storage.Storage;
import resultsview.storage.Job;
import resultsview.storage.Run;
import resultsview.storage.Pkg;
import resultsview.common.VersionUtil;

public class ResultsViewStorageHandler extends SAXTreeHandler {
	
	public static final String SUPPORED_VERSION = "1.0";
	public Storage storage;
	
	boolean skip;
	private Job job;
	
	public ResultsViewStorageHandler(Storage storage) {
		this.storage = storage;
		init();
	}

    private void init() {
    	SAXTreeHandlerNode rootNode = new SAXTreeHandlerNode(null);
    	SAXTreeHandlerNode resultsviewNode = new SAXTreeHandlerNode("resultsviewstorage") {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            	String version = attributes.getValue("version");
            	skip = VersionUtil.versionCompare(version, SUPPORED_VERSION) > 0;
            }
            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
            }
    	};
    	SAXTreeHandlerNode jobsNode = new SAXTreeHandlerNode("jobs");
    	SAXTreeHandlerNode jobNode = new SAXTreeHandlerNode("job") {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            	if (skip) {
            		return;
            	}
            	String name = attributes.getValue("name");
            	if (name == null) {
            		return;
            	}
				job = storage.getJob(name);
				if (job == null) {
					job = new Job(name);
					storage.storeJob(job);
				}
            }
            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                job = null;
            }
    	};
    	SAXTreeHandlerNode runNode = new SAXTreeHandlerNode("run") {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            	if (skip) {
            		return;
            	}
            	if (job == null) {
            		return;
            	}
            	String id = attributes.getValue("id");
            	if (id == null) {
            		return;
            	}
            	Run run = storage.getRun(job.getName(), id);
            	if (run != null) {
            		return; // already exists
            	}
            	id = Run.internId(id);
            	run = new Run(job, id);
            	String statusStr = attributes.getValue("status");
            	int status = statusStr != null ? Run.getStatus(statusStr) : Run.RUNNING;
            	run.setStatus(status);
            	storage.storeRun(run);
            	if (status == Run.RUNNING || status == Run.UNKNOWN) {
            		storage.addUnfinishedRun(run);
            	}
            	Run latestRun = storage.getJobLatestRun(job);
            	if (latestRun == null || run.compareTo(latestRun) > 0) {
            		storage.setJobLatestRun(job, run);
            	}
            	String modifTimeStr = attributes.getValue("modifTime");
            	if (modifTimeStr != null) {
            		run.modifTime = Long.valueOf(modifTimeStr);
            	}
            	String startTimeStr = attributes.getValue("startTime");
            	if (startTimeStr != null) {
            		run.setStartTime(Long.valueOf(startTimeStr));
            	}
            	String pkgName = attributes.getValue("pkg");
            	if (pkgName != null) {
            		Pkg pkg = storage.getPkg(pkgName);
            		if (pkg == null) {
            			pkg = new Pkg(pkgName);
            			storage.storePkg(pkg);
            		}
            		run.setPkg(pkg);
            		storage.addPkgRun(pkg, run);
            	}
            }
            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
            }
    	};
    	rootNode.addChild(resultsviewNode.getName(), resultsviewNode);
    	resultsviewNode.addChild(jobsNode.getName(), jobsNode);
    	jobsNode.addChild(jobNode.getName(), jobNode);
    	jobNode.addChild(runNode.getName(), runNode);
    	setRootNode(rootNode);
    }

}
