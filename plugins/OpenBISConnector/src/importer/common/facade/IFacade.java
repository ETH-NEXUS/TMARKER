/*
 * Copyright 2015 ETH Zurich, Scientific IT Services
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

package importer.common.facade;

import java.io.File;
import java.util.List;

import ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.FileInfoDssDTO;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;

/**
 * @author pkupczyk
 */
public interface IFacade
{

    boolean isSessionActive();

    List<Sample> listSamples();

    List<DataSet> listDataSets();

    List<DataSet> listDataSetsBySample(String samplePermId);
    
    File downloadDataSet(String dataSetCode);

    //
    // Optional - Methods to implement to allow separate file downloads from a dataset
    //
    
    boolean isDownloadDataSetFileAvailable();
    
    File downloadDataSetFile(String dataSetCode, String pathInDataSet);
    
    boolean isGetAllDataSetFilesAvailable();
    
    FileInfoDssDTO[] getAllDataSetFiles(String dataSetCode);
}