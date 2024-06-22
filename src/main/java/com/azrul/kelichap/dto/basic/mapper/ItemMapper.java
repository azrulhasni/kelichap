/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.dto.basic.mapper;

import com.azrul.kelichap.domain.DocumentData;
import com.azrul.kelichap.domain.Folder;
import com.azrul.kelichap.domain.FolderAccess;
import com.azrul.kelichap.domain.FolderRight;
import com.azrul.kelichap.domain.Item;
import com.azrul.kelichap.domain.Note;
import com.azrul.kelichap.dto.basic.DocumentDataDTO;
import com.azrul.kelichap.dto.basic.FolderDTO;
import com.azrul.kelichap.dto.basic.ItemDTO;
import com.azrul.kelichap.dto.basic.ItemProxyDTO;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.TypeToken;
import org.modelmapper.config.Configuration;
import org.modelmapper.spi.MappingContext;

/**
 *
 * @author azrul
 */
public class ItemMapper {

    public static void main(String[] args) {
        ItemMapper i = new ItemMapper();
        i.run();
    }
    

    private void run() {
        LocalDateTime date = LocalDateTime.now();
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE);
        
        Folder rootFolder = new Folder();
        rootFolder.setId(10000L);
        rootFolder.setName("my folder 1");
        rootFolder.setAccessMap(Set.of( new FolderAccess("goofy.goof", FolderRight.FOLDER_OWNER),
                                    new FolderAccess("mickey.mouse",FolderRight.CREATE_UPDATE_FOLDER),
                                    new FolderAccess("daisy.duck",FolderRight.READ_FOLDER)));
        
        rootFolder.setActive(Boolean.TRUE);
        rootFolder.setCreatedBy("goofy.goof");
        rootFolder.setCreationDate(date.minusDays(1L));
        rootFolder.setLastModifiedBy("mickey.mouse");
        rootFolder.setLastModifiedDate(date);
        rootFolder.setLevel(0);
        rootFolder.setParent(null);
        rootFolder.setRoot(rootFolder);
        rootFolder.setVersion(1);
        rootFolder.setWorkflowId("001");
        
        Folder parentFolder = new Folder();
        parentFolder.setId(11000L);
        parentFolder.setName("my folder 1 1");
        parentFolder.setAccessMap(Set.of( new FolderAccess("mnnie.mouse", FolderRight.FOLDER_OWNER),
                                    new FolderAccess("mickey.mouse",FolderRight.CREATE_UPDATE_FOLDER),
                                    new FolderAccess("daisy.duck",FolderRight.READ_FOLDER)));
        
        parentFolder.setActive(Boolean.TRUE);
        parentFolder.setCreatedBy("minnie.mouse");
        parentFolder.setCreationDate(date.minusDays(1L));
        parentFolder.setLastModifiedBy("goofy.goof");
        parentFolder.setLastModifiedDate(date);
        parentFolder.setLevel(1);
        parentFolder.setParent(rootFolder);
        parentFolder.setRoot(rootFolder);
        parentFolder.setVersion(1);
        
        rootFolder.getChildren().add(parentFolder);
        
        Folder child1Folder = new Folder();
        child1Folder.setId(11100L);
        child1Folder.setName("my folder 1 1 1");
        child1Folder.setAccessMap(Set.of( new FolderAccess("mnnie.mouse", FolderRight.FOLDER_OWNER),
                                    new FolderAccess("mickey.mouse",FolderRight.CREATE_UPDATE_FOLDER),
                                    new FolderAccess("daisy.duck",FolderRight.READ_FOLDER)));
        
        child1Folder.setActive(Boolean.TRUE);
        child1Folder.setCreatedBy("minnie.mouse");
        child1Folder.setCreationDate(date.minusDays(1L));
        child1Folder.setLastModifiedBy("goofy.goof");
        child1Folder.setLastModifiedDate(date);
        child1Folder.setLevel(2);
        child1Folder.setParent(parentFolder);
        child1Folder.setRoot(rootFolder);
        child1Folder.setVersion(1);
        
        parentFolder.getChildren().add(child1Folder);
        
        Folder child2Folder = new Folder();
        child2Folder.setId(11200L);
        child2Folder.setName("my folder 1 1 2");
        child2Folder.setAccessMap(Set.of( new FolderAccess("mnnie.mouse", FolderRight.FOLDER_OWNER),
                                    new FolderAccess("mickey.mouse",FolderRight.CREATE_UPDATE_FOLDER),
                                    new FolderAccess("daisy.duck",FolderRight.READ_FOLDER)));
        
        child2Folder.setActive(Boolean.TRUE);
        child2Folder.setCreatedBy("minnie.mouse");
        child2Folder.setCreationDate(date.minusDays(1L));
        child2Folder.setLastModifiedBy("goofy.goof");
        child2Folder.setLastModifiedDate(date);
        child2Folder.setLevel(2);
        child2Folder.setParent(parentFolder);
        child2Folder.setRoot(rootFolder);
        child2Folder.setVersion(1);
        
        parentFolder.getChildren().add(child2Folder);
        
        DocumentData doc1 = new DocumentData();
        doc1.setId(11110L);
        doc1.setName("my doc 1 1 1 1");
        doc1.setCreatedBy("minnie.mouse");
        doc1.setCreationDate(date.minusDays(1L));
        doc1.setLastModifiedBy("goofy.goof");
        doc1.setLastModifiedDate(date);
        doc1.setLevel(3);
        doc1.setParent(child1Folder);
        doc1.setRoot(rootFolder);
        doc1.setVersion(1);
        
        Note note11 = new Note();
        note11.setId(11111L);
        note11.setMessage("Hello from 11111");
        note11.setWriterUserName("daisy.duck");
        note11.setWrittenDate(LocalDateTime.now());
        doc1.getNotes().add(note11);
        
        Note note12 = new Note();
        note12.setId(11112L);
        note12.setMessage("Hello from 11112");
        note12.setWriterUserName("goofy.goof");
        note12.setWrittenDate(LocalDateTime.now());
        doc1.getNotes().add(note12);
        
        
        child1Folder.getChildren().add(doc1);
        
        DocumentData doc2 = new DocumentData();
        doc2.setId(11120L);
        doc2.setName("my doc 1 1 1 2");
        doc2.setCreatedBy("donald.duck");
        doc2.setCreationDate(date.minusDays(1L));
        doc2.setLastModifiedBy("minnie.mouse");
        doc2.setLastModifiedDate(date);
        doc2.setLevel(3);
        doc2.setParent(child1Folder);
        doc2.setRoot(rootFolder);
        doc2.setVersion(1);
        
        
        
        child1Folder.getChildren().add(doc2);
       
//        
//       Converter<Set<Item>, Set<Long>> ItemSetToIdSetConverter =
//        ctx -> ctx.getSource()
//                .stream()
//                .map(Item::getId)
//                .collect(Collectors.toSet());
//
//        modelMapper.createTypeMap(Folder.class, FolderDTO.class)
//        .addMappings(map -> map
//                .using(ItemSetToIdSetConverter)
//                .map(
//                        Folder::getChildren,
//                        FolderDTO::setChildrenId
//                )
//        );

           
       Converter<Set<Item>, Set<ItemProxyDTO>> ItemSetToIdSetConverter =
        ctx -> ctx.getSource()
                .stream()
                .map(item->modelMapper.map(item,ItemProxyDTO.class))
                .collect(Collectors.toSet());

//        modelMapper.createTypeMap(Folder.class, FolderDTO.class)
//        .addMappings(map -> map
//                .using(ItemSetToIdSetConverter)
//                .map(
//                        Folder::getChildren,
//                        FolderDTO::setChildren
//                )
//        );
         FolderDTO rootDTO = modelMapper.map(rootFolder, FolderDTO.class);
         FolderDTO parentDTO = modelMapper.map(parentFolder, FolderDTO.class);
         DocumentDataDTO docDTO = modelMapper.map(doc1, DocumentDataDTO.class);
        
        System.out.println(rootDTO);
        System.out.println("=========");
        System.out.println(parentDTO);
        System.out.println("=========");
        System.out.println(docDTO);
    }

}
