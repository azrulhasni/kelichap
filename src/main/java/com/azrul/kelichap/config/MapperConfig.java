/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.config;

import com.azrul.kelichap.domain.DocumentData;
import com.azrul.kelichap.domain.Folder;
import com.azrul.kelichap.domain.Item;
import com.azrul.kelichap.domain.Note;
import com.azrul.kelichap.dto.basic.DocumentDataDTO;
import com.azrul.kelichap.dto.basic.DocumentDataProxyDTO;
//import com.azrul.kelichap.dto.basic.DocumentDataProxyDTO;
import com.azrul.kelichap.dto.basic.FolderDTO;
import com.azrul.kelichap.dto.basic.FolderProxyDTO;
//import com.azrul.kelichap.dto.basic.FolderProxyDTO;
import com.azrul.kelichap.dto.basic.ItemDTO;
import com.azrul.kelichap.dto.basic.ItemProxyDTO;
import com.azrul.kelichap.dto.basic.NoteDTO;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.spi.DestinationSetter;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author azrul
 */
@Configuration
public class MapperConfig {

     @Bean("FolderMapper")
    public ModelMapper getFolderMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PUBLIC);
                
        ModelMapper proxyModelMapper = new ModelMapper();
        proxyModelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);

        Converter<DocumentData[], DocumentDataProxyDTO[]> docSetToDocProxySetConverter = new Converter<>(){
            @Override
            public DocumentDataProxyDTO[] convert(MappingContext<DocumentData[], DocumentDataProxyDTO[]> mc) {
                return Arrays.asList(mc.getSource()).stream()
                        .map(doc -> {
                            DocumentDataProxyDTO dto = proxyModelMapper.map(doc, DocumentDataProxyDTO.class);
                            return dto;
                        })
                        .toArray(DocumentDataProxyDTO[]::new);
            }
        };
        
        Converter<Folder[], FolderProxyDTO[]> folderSetToFolderProxySetConverter = new Converter<>(){
            @Override
            public FolderProxyDTO[] convert(MappingContext<Folder[], FolderProxyDTO[]> mc) {
                return Arrays.asList(mc.getSource()).stream()
                        .map(folder -> {
                             FolderProxyDTO dto = proxyModelMapper.map(folder, FolderProxyDTO.class);
                            return dto;
                        })
                        .toArray(FolderProxyDTO[]::new);
            }
        };
        modelMapper.addConverter(folderSetToFolderProxySetConverter);
        modelMapper.addConverter(docSetToDocProxySetConverter);
        modelMapper.createTypeMap(Folder.class, FolderDTO.class)
                .addMappings(map -> map.skip(FolderDTO::setChildren))
                .addMappings(map -> map
                    .map(
                            Folder::getFolderChildren,
                            FolderDTO::setFolderChildren
                    ))
                .addMappings(map -> map
                    .map(
                            Folder::getDocumentChildren,
                            FolderDTO::setDocumentChildren
                    ));
                

        return modelMapper;
    }
    
     @Bean("DocMapper")
    public ModelMapper getDocumentMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PUBLIC);
        return modelMapper;
    }
    
     @Bean("BasicMapper")
    public ModelMapper getBasicMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PUBLIC);
        return modelMapper;
    }
   
}
