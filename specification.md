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
         u8 launcher_metadata_offset;
         u8 boot_metadata_offset;
         u1[24] reserved;
    } end;
}
```

## boot

[BootMetadata](boot/src/main/java/org/glavo/japp/boot/JAppBootMetadata.java):

```
BootMetadata {
    u4 group_count;
    ResourceGroup[group_count] groups;
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
    u2 flags;
    
    u4 uncompressed_size;
    u4 compressed_size;
    u8 content_offset;
    
    u2 path_length;
    u1[path_length] path; // UTF-8  
    
    ResourceFields optional_fields;
}
```

## launcher

TODO

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
