# Specification (Draft)

File format:

```
JAppFile {
    u4 magic_number; // "JAPP"
    u1[...] data_pool;
    BootMetadata boot_metadata;
    LauncherMetadata launcher_metadata;
    FileEnd {
         u4 magic_number; // "JAPP"
         u2 major_version;
         u2 minor_version;
         u8 flags;
         u8 file_size;
         u8 boot_metadata_offset;
         u8 launcher_metadata_offset;
         u1[24] reserved;
    } end;
}
```

## boot

[BootMetadata](boot/src/main/java/org/glavo/japp/boot/JAppBootMetadata.java):

```
BootMetadata {
    u4 magic_number; // "BOOT"
    u4 group_count;
    ByteArrayPool stringsPool;
    ResourceGroup[group_count] groups;
}
```

[ByteArrayPool]():

```
ByteArrayPool {
    u1 magic_number; // 0xf0
    u1 compress_method;
    u2 resvered;
    u4 count;
    u4 uncompressed_bytes_size;
    u4 compressed_bytes_size;
    u2[count] sizes; 
    u1[compressed_length] compressed_bytes;
}
```

[ResourceGroup](boot/src/main/java/org/glavo/japp/boot/JAppResourceGroup.java):

```
ResourceGroup {
    u1 magic_number; // 0xeb
    u1 compress_method;
    u2 reserved;
    
    u4 uncompressed_size;
    u4 compressed_size;
    u4 resources_count;
    u8 checksum;
    
    u1[compressed_size] compressed_resources;
}
```

[Resource](boot/src/main/java/org/glavo/japp/boot/JAppResource.java):

```
Resource {
    u1 magic_number; // 0x1b
    u1 compress_method;
    u2 path_length;
    u4 reserved;
    u8 uncompressed_size;
    u8 compressed_size;
    u8 content_offset;
    u1[path_length] path; // UTF-8  
    
    ResourceFields optional_fields;
}
```

## launcher

[LauncherMetadata](src/main/java/org/glavo/japp/launcher/JAppLauncherMetadata.java):

```
LauncherMetadata {
    ConfigGroup root_group;
}
```

[ConfigGroup](src/main/java/org/glavo/japp/JAppConfigGroup.java):

```
ConfigGroup {
    u4 magic_number; // "GRP\0"
    ConfigGroupFieldList fields;
}
```

[ResourceGroupReference](src/main/java/org/glavo/japp/JAppResourceGroupReference.java):

```
ResourceGroupReference {
    u1 id;
    String name;
    union {
        LocalResourceGroupReference local_reference;
        MavenResourceGroupReference maven_reference;
    } reference;
}

LocalResourceGroupReference {
    
    u4 index;
    u4 multi_count;
    {
        u4 multi_version;
        u4 multi_index;  
    }[multi_count] multi_index_pairs;
}

MavenResourceGroupReference {
    String repository;
    String group;
    String artifact;
    String version;
    String classifier;
}
```

## share

Pass module path and class path:

```
PathList  : PathItem (',' PathItem)*
PathItem  : Name ':' PathValue
PathValue : 'E' ExternalPath 
          | GroupIndexList

GroupIndexList : GroupIndex ('+' GroupIndex)*
GroupIndex: [0-9a-f]+ 
```
