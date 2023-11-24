## launcher

## boot

[BootMetadata](boot/src/main/java/org/glavo/japp/boot/JAppBootMetadata.java):

```
 BootMetadata {
     u4 pool_length;
     u4 group_count;
     u4[group_count] group_lengths;
 
     u1[pool_length] pool;
     ResourceGroup[group_count] groups;
 }
```

[ResourceGroup](boot/src/main/java/org/glavo/japp/boot/JAppResourceGroup.java)

```
TODO
```

## share

Module path and class path:

```
PathList  : PathItem (',' PathItem)*
PathItem  : Name ':' PathValue
PathValue : 'E' ExternalPath 
          | GroupIndexList

GroupIndexList : GroupIndex ('+' GroupIndex)*
GroupIndex: [0-9a-f]+ 
```
