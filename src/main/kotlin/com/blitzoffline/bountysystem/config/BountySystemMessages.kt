package com.blitzoffline.bountysystem.config

import ch.jalu.configme.SettingsManagerImpl
import ch.jalu.configme.configurationdata.ConfigurationDataBuilder
import ch.jalu.configme.migration.PlainMigrationService
import ch.jalu.configme.resource.YamlFileResource
import com.blitzoffline.bountysystem.config.sections.Messages
import java.io.File

class BountySystemMessages(file: File) : SettingsManagerImpl(
    YamlFileResource(file),
    ConfigurationDataBuilder.createConfiguration(Messages::class.java),
    PlainMigrationService()
)