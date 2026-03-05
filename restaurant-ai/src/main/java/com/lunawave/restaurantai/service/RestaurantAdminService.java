package com.lunawave.restaurantai.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lunawave.restaurantai.db.RestaurantDocument;
import com.lunawave.restaurantai.db.RestaurantDocumentId;
import com.lunawave.restaurantai.repo.RestaurantDocumentRepository;

@Service
public class RestaurantAdminService {

    private final RestaurantDocumentRepository docRepo;

    public RestaurantAdminService(RestaurantDocumentRepository docRepo) {
        this.docRepo = docRepo;
    }

    public List<RestaurantDocument> listDocs(String restaurantId) {
        return docRepo.findByIdRestaurantId(restaurantId);
    }

    public Optional<RestaurantDocument> getDoc(String restaurantId, String docId) {
        return docRepo.findById(new RestaurantDocumentId(restaurantId, docId));
    }

    @Transactional
    public RestaurantDocument upsertDoc(String restaurantId, String docId, String type, String title, String text, String metadataJson) {
        var id = new RestaurantDocumentId(restaurantId, docId);

        var existing = docRepo.findById(id);
        RestaurantDocument doc = existing.orElseGet(() -> new RestaurantDocument(id, type, title, text, metadataJson));

        doc.setDocType(type);
        doc.setTitle(title);
        doc.setText(text);
        doc.setMetadataJson(metadataJson);

        // Later: compute contentHash, and sync to Chroma here (delete old vectors + add new)
        return docRepo.save(doc);
    }

    @Transactional
    public void deleteDoc(String restaurantId, String docId) {
        docRepo.deleteById(new RestaurantDocumentId(restaurantId, docId));
        // Later: delete vectors in Chroma for (restaurantId, docId)
    }

    @Transactional
    public void deleteAllDocs(String restaurantId) {
        docRepo.deleteByIdRestaurantId(restaurantId);
        // Later: delete ALL vectors in Chroma for restaurantId
    }
}