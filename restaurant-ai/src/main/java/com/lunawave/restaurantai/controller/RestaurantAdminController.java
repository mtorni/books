package com.lunawave.restaurantai.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lunawave.restaurantai.db.RestaurantDocument;
import com.lunawave.restaurantai.service.RestaurantAdminService;

@RestController
@RequestMapping("/api/admin/restaurants/{restaurantId}")
public class RestaurantAdminController {

    private final RestaurantAdminService adminService;

    public RestaurantAdminController(RestaurantAdminService adminService) {
        this.adminService = adminService;
    }

    // GET /api/admin/restaurants/{restaurantId}/docs
    @GetMapping("/docs")
    public Map<String, Object> listDocs(@PathVariable String restaurantId) {
        List<RestaurantDocument> docs = adminService.listDocs(restaurantId);

        var list = docs.stream()
            .map(d -> Map.of(
                "docId", d.getId().getDocId(),
                "type", d.getDocType(),
                "title", d.getTitle()
            ))
            .toList();

        return Map.of(
            "ok", true,
            "restaurantId", restaurantId,
            "count", list.size(),
            "docs", list
        );
    }

 // GET /api/admin/restaurants/{restaurantId}/docs/{docId}
    @GetMapping("/docs/{docId}")
    public Map<String, Object> getDoc(@PathVariable String restaurantId, @PathVariable String docId) {

        var opt = adminService.getDoc(restaurantId, docId);

        if (opt.isEmpty()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("ok", false);
            resp.put("message", "Doc not found");
            resp.put("restaurantId", restaurantId);
            resp.put("docId", docId);
            return resp;
        }

        var d = opt.get();

        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        resp.put("restaurantId", restaurantId);
        resp.put("docId", d.getId().getDocId());
        resp.put("type", d.getDocType());
        resp.put("title", d.getTitle());
        resp.put("text", d.getText());
        resp.put("metadataJson", d.getMetadataJson());
        return resp;
    }

    // POST /api/admin/restaurants/{restaurantId}/docs  (single-doc upsert for now)
    @PostMapping("/docs")
    public Map<String, Object> upsertDoc(@PathVariable String restaurantId, @RequestBody UpsertDocRequest req) {

        RestaurantDocument saved = adminService.upsertDoc(
            restaurantId,
            req.docId(),
            req.type(),
            req.title(),
            req.text(),
            req.metadataJson()
        );

        return Map.of(
            "ok", true,
            "message", "Saved",
            "restaurantId", restaurantId,
            "docId", saved.getId().getDocId()
        );
    }

    // DELETE /api/admin/restaurants/{restaurantId}/docs/{docId}
    @DeleteMapping("/docs/{docId}")
    public Map<String, Object> deleteDoc(@PathVariable String restaurantId, @PathVariable String docId) {
        adminService.deleteDoc(restaurantId, docId);
        return Map.of("ok", true, "message", "Deleted", "restaurantId", restaurantId, "docId", docId);
    }

    // DELETE /api/admin/restaurants/{restaurantId}/docs
    @DeleteMapping("/docs")
    public Map<String, Object> deleteAll(@PathVariable String restaurantId) {
        adminService.deleteAllDocs(restaurantId);
        return Map.of("ok", true, "message", "Deleted all docs", "restaurantId", restaurantId);
    }

    public record UpsertDocRequest(
        String docId,
        String type,
        String title,
        String text,
        String metadataJson
    ) {}
}