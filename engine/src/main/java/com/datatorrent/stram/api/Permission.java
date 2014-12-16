/**
 * Copyright (c) 2012-2014 DataTorrent, Inc. All rights reserved.
 */
package com.datatorrent.stram.api;

/**
 *
 * @author David Yan <david@datatorrent.com>
 */


public enum Permission
{
  /**
   * View applications launched by others
   */
  VIEW_OTHER_USERS_APPS,
  /**
   * Manage applications launched by others
   */
  MANAGE_OTHER_USERS_APPS,
  /**
   * View global settings
   */
  VIEW_GLOBAL_CONFIG,
  /**
   * Edit global settings
   */
  EDIT_GLOBAL_CONFIG,
  /**
   * Access proxy to RM
   */
  ACCESS_RM_PROXY,
  /**
   * View licenses
   */
  VIEW_LICENSES,
  /**
   * Manage licenses
   */
  MANAGE_LICENSES,
  /**
   * Launch App Packages
   */
  LAUNCH_APPS,
  /**
   * Upload App Packages
   */
  UPLOAD_APP_PACKAGES,
  /**
   * View other users' App Packages
   */
  VIEW_OTHER_USERS_APP_PACKAGES,
  /**
   * Manage other users' App Packages
   */
  MANAGE_OTHER_USERS_APP_PACKAGES,
  /**
   * Manage users (create/delete users, change password)
   */
  MANAGE_USERS,
  /**
   * Manage roles (create/delete roles)
   */
  MANAGE_ROLES,
  /**
   * View system alerts
   */
  VIEW_SYSTEM_ALERTS,
  /**
   * Manage system alerts
   */
  MANAGE_SYSTEM_ALERTS
}
