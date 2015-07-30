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
import java.io.FileFilter;
import java.io.InputStream;
import java.util.List;

import common.config.Config;
import ch.systemsx.cisd.common.exceptions.InvalidSessionException;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet;
import ch.systemsx.cisd.openbis.dss.client.api.v1.IOpenbisServiceFacade;
import ch.systemsx.cisd.openbis.dss.client.api.v1.impl.OpenbisServiceFacade;
import ch.systemsx.cisd.openbis.dss.generic.shared.api.v1.FileInfoDssDTO;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SearchCriteria;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SearchCriteria.MatchClause;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.SearchCriteria.MatchClauseAttribute;

/**
 * @author pkupczyk
 */
public abstract class AbstractFacade implements IFacade
{

    protected Config config;

    protected IOpenbisServiceFacade service;
    
    public Exception loginException = null;

    public AbstractFacade(Config config)
    {
        this.config = config;
        try {
            service = OpenbisServiceFacade.tryCreate(config.getUser(), config.getPassword(), config.getUrl(), config.getTimeout());
        } catch (Exception e) {
            loginException = e;
            service = null;
        }
    }

    @Override
    public boolean isSessionActive()
    {
        try
        {
            service.checkSession();
            return true;
        } catch (InvalidSessionException e)
        {
            return false;
        }
    }

    protected File downloadDataSet(String code, FileFilter filter)
    {
        SearchCriteria criteria = new SearchCriteria();
        criteria.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.PERM_ID, code));
        List<DataSet> dataSets = service.searchForDataSets(criteria);

        if (dataSets.size() > 0)
        {
            DataSet dataSet = dataSets.get(0);
            File downloadFolder = new File(config.getWorkspace(), code);

            if (downloadFolder.exists())
            {
                System.out.println("Data set '" + code + "' has been already downloaded. Will just check if the files match.");
            } else
            {
                System.out.println("Data set '" + code + "' has never been downloaded. Will download it.");
                downloadFolder.mkdirs();
            }

            FileInfoDssDTO[] files = dataSet.listFiles("/", false);
            downloadFiles(dataSet, downloadFolder, files, filter);
            return getDownloadedFileOrFolder(downloadFolder, filter);
        }
        return null;
    }

    private void downloadFiles(DataSet dataSet, File downloadFolder, FileInfoDssDTO[] fileDTOs, FileFilter filter)
    {
        if (fileDTOs.length > 0)
        {
            for (FileInfoDssDTO fileDTO : fileDTOs)
            {
                File file = new File(downloadFolder, fileDTO.getPathInDataSet());
                if (filter != null && !filter.accept(file) && !fileDTO.isDirectory())
                {
                    System.out.println("Ignoring file '" + fileDTO.getPathInDataSet() + "'");
                    continue;
                }
                
                if (fileDTO.isDirectory())
                {
                    if (file.exists())
                    {
                        System.out.println("Directory '" + fileDTO.getPathInDataSet() + "' already exists.");
                    } else
                    {
                        System.out.println("Creating directory '" + fileDTO.getPathInDataSet() + "'.");
                        file.mkdir();
                    }

                    FileInfoDssDTO[] childDTOs = dataSet.listFiles(fileDTO.getPathInDataSet(), false);
                    downloadFiles(dataSet, downloadFolder, childDTOs, filter);
                } else
                {
                    if (file.exists())
                    {
                        if (file.length() == fileDTO.getFileSize())
                        {
                            System.out.println("File '" + fileDTO.getPathInDataSet() + "' already exists and has the same size. Do nothing.");
                        } else
                        {
                            System.out.println("File '" + fileDTO.getPathInDataSet() + "' already exists but has a different size. Will replace it.");
                            file.delete();
                            InputStream stream = dataSet.getFile(fileDTO.getPathInDataSet());
                            FileUtilities.writeToFile(file, 0L, stream);
                        }
                    } else
                    {
                        System.out.println("Downloading file '" + fileDTO.getPathInDataSet() + "'.");
                        InputStream stream = dataSet.getFile(fileDTO.getPathInDataSet());
                        FileUtilities.writeToFile(file, 0L, stream);
                    }
                }
            }
        }
    }

    private File getDownloadedFileOrFolder(File downloadFolder, FileFilter filter)
    {
        List<File> files = FileUtilities.listFiles(downloadFolder, filter, true);

        if (files != null && files.size() > 0)
        {
            if (files.size() == 1)
            {
                return files.get(0);
            } else
            {
                return downloadFolder;
            }
        } else
        {
            return null;
        }
    }

    //
    // Optional Methods - Default implementation that can be used by many Facades
    //
    
    public File downloadDataSetFile(final String dataSetCode, final String pathInDataSet) {
        return downloadDataSet(dataSetCode, new FileFilter()
            {
                @Override
                public boolean accept(File file)
                {
                    String absolutePath = file.getAbsolutePath();
                    boolean accept = absolutePath.endsWith(pathInDataSet);
                    return accept;
                }
            });
    }
	
    @Override
    public FileInfoDssDTO[] getAllDataSetFiles(String code)
    {
        SearchCriteria criteria = new SearchCriteria();
        criteria.addMatchClause(MatchClause.createAttributeMatch(MatchClauseAttribute.PERM_ID, code));
        List<DataSet> dataSets = service.searchForDataSets(criteria);

        if (dataSets.size() > 0)
        {
            DataSet dataSet = dataSets.get(0);
            return dataSet.listFiles("/", true);
        }
        return null;
    }
}