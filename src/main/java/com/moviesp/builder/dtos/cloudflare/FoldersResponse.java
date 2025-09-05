package com.moviesp.builder.dtos.cloudflare;

import com.moviesp.builder.dtos.Folder;

import java.util.List;

public record FoldersResponse (boolean success, List<Folder> results){
}
