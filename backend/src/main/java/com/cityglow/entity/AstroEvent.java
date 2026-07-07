package com.cityglow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 天文事件实体,对应 astro_events 表。
 *
 * <p>event_type 取值:METEOR(流星雨)、ECLIPSE(日月食)、PLANET(行星合)等。</p>
 */
@Entity
@Table(name = "astro_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AstroEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_type", length = 20)
    private String eventType;
}
