package com.safaricom.taskmanager.repository;

import com.safaricom.taskmanager.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
}
