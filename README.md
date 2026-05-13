# MStorage

Advanced material compression storage plugin for Paper / Purpur servers.

---

# Supported Versions

* Paper 1.21.11
* Purpur 1.21.11
* Paper 1.26.12
* Purpur 1.26.12

---

# Requirements

* Java 21+
* Paper or Purpur server

---

# Features

* Single material compression storage
* Exact ItemStack matching
* Auto collect system
* HAND mode
* CONTAINER mode
* Grindstone de-storage
* PDC-based safe storage system
* Rollback-safe transaction handling
* TPS protection system
* Storage item protection
* Container bulk storage support
* Real-time lore and display updates

---

# Storage Rules

* Only one exact item type can be stored per storage item
* Storage items cannot store other storage items
* Exact matching uses:

  * Material
  * ItemMeta
  * PDC
* Damage value is ignored

---

# Auto Collect

* Automatically collects nearby matching items
* Collection radius: 4 blocks
* Toggle ON/OFF supported
* TPS-safe throttled processing
* Storage items are excluded

---

# Modes

## HAND Mode

Interact directly while holding the storage item.

## CONTAINER Mode

Store matching items into containers automatically.

---

# De-Storage

Use a grindstone to safely remove storage status and restore the original item.

---

# Commands

| Command          | Description          |
| ---------------- | -------------------- |
| /mstorage reload | Reload configuration |

---

# Permissions

| Permission      | Description                 | Default |
| --------------- | --------------------------- | ------- |
| mstorage.admin  | Administrative access       | OP      |
| mstorage.reload | Reload plugin configuration | OP      |

---

# Safety Features

* Overflow rollback protection
* Invalid item validation
* Container safety checks
* Inventory conflict prevention
* Item duplication prevention
* Auto collect safety filtering
* Owner pickup protection
* TPS-aware throttling

---

# Notes

* Designed for Paper/Purpur servers only
* Unsupported inventory modification plugins may cause conflicts
* Always test on a backup server before production use

---

# Development Environment

* Java 21
* Maven
* IntelliJ IDEA
* Paper API

---

# License

Private development repository.
All rights reserved.
