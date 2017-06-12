/**********************************************************************************
 * $URL: https://source.etudes.org/svn/apps/mysites/trunk/mysites-webapp/webapp/src/java/org/etudes/mysites/cdp/MySitesCdpHandler.java $
 * $Id: MySitesCdpHandler.java 8502 2014-08-22 02:02:37Z ggolden $
 ***********************************************************************************
 *
 * Copyright (c) 2013, 2014 Etudes, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.etudes.mysites.cdp;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.archives.api.ArchiveDescription;
import org.etudes.archives.api.ArchivesService;
import org.etudes.cdp.api.CdpHandler;
import org.etudes.cdp.api.CdpStatus;
import org.etudes.cdp.util.CdpResponseHelper;
import org.etudes.util.DateHelper;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.javax.PagingPosition;
import org.sakaiproject.site.api.PubDatesService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteInfo;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.SiteService.SelectionType;
import org.sakaiproject.site.api.SiteService.SortType;
import org.sakaiproject.user.api.Preferences;
import org.sakaiproject.user.api.PreferencesEdit;
import org.sakaiproject.user.api.PreferencesService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.StringUtil;

/**
 */
public class MySitesCdpHandler implements CdpHandler
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(MySitesCdpHandler.class);

	public String getPrefix()
	{
		return "mysites";
	}

	public Map<String, Object> handle(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String requestPath,
			String path, String authenticatedUserId) throws ServletException, IOException
	{
		// if no authenticated user, we reject all requests
		if (authenticatedUserId == null)
		{
			Map<String, Object> rv = new HashMap<String, Object>();
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.notLoggedIn.getId());
			return rv;
		}

		else if (requestPath.equals("setFavoriteSite"))
		{
			return dispatchSetFavoriteSite(req, res, parameters, path, authenticatedUserId);
		}
		else if (requestPath.equals("publishSites"))
		{
			return dispatchPublishSites(req, res, parameters, path);
		}
		else if (requestPath.equals("unpublishSites"))
		{
			return dispatchUnpublishSites(req, res, parameters, path);
		}
		else if (requestPath.equals("archives"))
		{
			return dispatchArchives(req, res, parameters, path, authenticatedUserId);
		}
		else if (requestPath.equals("allSites"))
		{
			return dispatchAllSites(req, res, parameters, path, authenticatedUserId);
		}
		else if (requestPath.equals("purge"))
		{
			return dispatchPurge(req, res, parameters, path, authenticatedUserId);
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> dispatchAllSites(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			String authenticatedUserId) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// admin / helpdesk only
		if ((!"admin".equals(authenticatedUserId)) && (!"helpdesk".equals(authenticatedUserId)))
		{
			M_log.warn("dispatchAllSites - not admin: " + authenticatedUserId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// optional search criteria
		String search = StringUtil.trimToNull((String) parameters.get("search"));

		// sort
		String sortStr = StringUtil.trimToNull((String) parameters.get("sort"));
		SortType sort = SortType.CREATED_ON_DESC;
		if ("term".equals(sortStr))
		{
			sort = SortType.TERM_DESC;
		}

		// page size
		String pageSizeStr = (String) parameters.get("pageSize");
		int pageSize = 200;
		if (pageSizeStr != null)
		{
			pageSize = Integer.parseInt(pageSizeStr);
		}

		// page
		String pageNumStr = (String) parameters.get("page");
		PagingPosition paging = new PagingPosition();
		if (pageNumStr != null)
		{
			// 1 based paging
			int pageNum = Integer.parseInt(pageNumStr) - 1;
			int first = (pageNum * pageSize) + 1;
			int last = first + pageSize - 1;
			paging.setPosition(first, last);
		}
		else
		{
			paging.setPaging(false);
		}

		// build up a map to return - the main map has a single "sites" object
		List<Map<String, Object>> sitesMap = new ArrayList<Map<String, Object>>();
		rv.put("sites", sitesMap);

		// all sites, with search criteria (on title)
		int count = siteService().countSites(org.sakaiproject.site.api.SiteService.SelectionType.NON_USER, null, search, null);

		// adjust the page request
		paging.validate(count);

		List<Site> sites = (List<Site>) siteService().getSites(org.sakaiproject.site.api.SiteService.SelectionType.NON_USER, null, search, null,
				sort, paging);
		for (Site site : sites)
		{
			// each site has a map
			Map<String, Object> siteMap = new HashMap<String, Object>();
			sitesMap.add(siteMap);

			loadSite(site, siteMap);
		}

		if (paging.isPaging())
		{
			rv.put("count", CdpResponseHelper.formatInt(count));
			rv.put("page", CdpResponseHelper.formatInt(((paging.getFirst() - 1) / pageSize) + 1));
			rv.put("first", CdpResponseHelper.formatInt(paging.getFirst()));
			rv.put("last", CdpResponseHelper.formatInt(paging.getLast()));
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	/**
	 * Dispatch the importSites request.
	 * 
	 * @param req
	 * @param res
	 * @param parameters
	 * @param path
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	protected Map<String, Object> dispatchArchives(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			String userId) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		List<ArchiveDescription> archives = archivesService().getUserArchives(userId);

		// return a "archives" array with each site and archive
		List<Map<String, String>> sitesList = new ArrayList<Map<String, String>>();
		rv.put("archives", sitesList);

		for (ArchiveDescription a : archives)
		{
			Map<String, String> siteMap = new HashMap<String, String>();
			sitesList.add(siteMap);

			siteMap.put("siteId", a.getSiteId());
			siteMap.put("title", a.getTitle());
			siteMap.put("term", "Archived: " + CdpResponseHelper.describeTerm(a.getTermDescription()));
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchPublishSites(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path)
			throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site ids parameter
		String siteIds = (String) parameters.get("siteIds");
		if (siteIds == null)
		{
			M_log.warn("dispatchPublishSites - no siteIds parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String[] ids = StringUtil.split(siteIds, "\t");
		for (String id : ids)
		{
			try
			{
				Site site = siteService().getSite(id);
				pubDatesService().processPublishOptions("publish", null, null, site, new SiteInfo());
			}
			catch (IdUnusedException e)
			{
				M_log.warn("dispatchPublishSites: id: " + id + " " + e.toString());
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchPurge(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path,
			String authenticatedUserId) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// admin only
		if (!"admin".equals(authenticatedUserId))
		{
			M_log.warn("dispatchPurge - not admin: " + authenticatedUserId);

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
			return rv;
		}

		// get the site ids parameter
		String siteIds = (String) parameters.get("siteIds");
		if (siteIds == null)
		{
			M_log.warn("dispatchPurge - no siteIds parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String[] ids = StringUtil.split(siteIds, "\t");
		for (String id : ids)
		{
			try
			{
				siteService().getSite(id);
				archivesService().purgeSiteNow(id);
			}
			catch (IdUnusedException e)
			{
				M_log.warn("dispatchPurge: id: " + id + " " + e.toString());

				// add status parameter
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}
			catch (PermissionException e)
			{
				M_log.warn("dispatchPurge: permission exception");

				// add status parameter
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
				return rv;
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> dispatchSetFavoriteSite(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters,
			String path, String userId) throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site id parameter
		// TODO: change this to favSiteId so as not to be confused with a context siteId for tracking...
		String siteId = (String) parameters.get("siteId");
		if (siteId == null)
		{
			M_log.warn("dispatchSetFavoriteSite - no siteId parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		// get the setting
		String favoriteStr = (String) parameters.get("favorite");
		if (favoriteStr == null)
		{
			M_log.warn("dispatchSetFavoriteSite - no favorite parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}
		boolean setting = (favoriteStr.equals("1"));

		// apply the setting to the user preferences
		Preferences prefs = preferencesService().getPreferences(userId);
		ResourceProperties props = prefs.getProperties("sakai:portal:sitenav");
		List<String> prefExclude = (List<String>) props.getPropertyList("exclude");
		List<String> prefOrder = (List<String>) props.getPropertyList("order");

		// to make a favorite, make sure it is NOT in this list - to remove a favorite, make sure it is.
		boolean needToSave = false;
		if (setting)
		{
			if ((prefExclude != null) && (prefExclude.contains(siteId)))
			{
				prefExclude.remove(siteId);
				needToSave = true;
			}
		}
		else
		{
			if ((prefExclude == null) || (!prefExclude.contains(siteId)))
			{
				if (prefExclude == null) prefExclude = new ArrayList<String>();
				prefExclude.add(siteId);

				// also need to remove it from the order list, if it is there
				if ((prefOrder != null) && (prefOrder.contains(siteId)))
				{
					prefOrder.remove(siteId);
				}
				needToSave = true;
			}
		}

		// save if we made a change
		if (needToSave)
		{
			try
			{
				PreferencesEdit edit = null;
				try
				{
					edit = preferencesService().edit(userId);
				}
				catch (IdUnusedException e)
				{
					// add a new one if this is the first time preferences will be set for the user
					edit = preferencesService().add(userId);
				}

				ResourcePropertiesEdit propsEdit = edit.getPropertiesEdit("sakai:portal:sitenav");

				propsEdit.removeProperty("exclude");
				if ((prefExclude != null) && (!prefExclude.isEmpty()))
				{
					for (String sid : prefExclude)
					{
						propsEdit.addPropertyToList("exclude", sid);
					}
				}

				propsEdit.removeProperty("order");
				if ((prefOrder != null) && (!prefOrder.isEmpty()))
				{
					for (String sid : prefOrder)
					{
						propsEdit.addPropertyToList("order", sid);
					}
				}

				preferencesService().commit(edit);
			}
			catch (PermissionException e)
			{
				M_log.warn("dispatchSetFavoriteSite: " + e);

				// add status parameter
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.accessDenied.getId());
				return rv;
			}
			catch (InUseException e)
			{
				M_log.warn("dispatchSetFavoriteSite: " + e);

				// add status parameter
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}
			catch (IdUsedException e)
			{
				M_log.warn("dispatchSetFavoriteSite: " + e);

				// add status parameter
				rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
				return rv;
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	protected Map<String, Object> dispatchUnpublishSites(HttpServletRequest req, HttpServletResponse res, Map<String, Object> parameters, String path)
			throws ServletException, IOException
	{
		Map<String, Object> rv = new HashMap<String, Object>();

		// get the site ids parameter
		String siteIds = (String) parameters.get("siteIds");
		if (siteIds == null)
		{
			M_log.warn("dispatchUnpublishSites - no siteIds parameter");

			// add status parameter
			rv.put(CdpStatus.CDP_STATUS, CdpStatus.badRequest.getId());
			return rv;
		}

		String[] ids = StringUtil.split(siteIds, "\t");
		for (String id : ids)
		{
			try
			{
				Site site = siteService().getSite(id);
				pubDatesService().processPublishOptions("unpublish", null, null, site, new SiteInfo());
			}
			catch (IdUnusedException e)
			{
				M_log.warn("dispatchUnpublishSites: id: " + id + " " + e.toString());
			}
		}

		// add status parameter
		rv.put(CdpStatus.CDP_STATUS, CdpStatus.success.getId());

		return rv;
	}

	@SuppressWarnings("unchecked")
	protected void loadSite(Site site, Map<String, Object> siteMap)
	{
		siteMap.put("siteId", site.getId());
		siteMap.put("title", site.getTitle());

		if (site.getShortDescription() != null) siteMap.put("description", site.getShortDescription());

		siteMap.put("published", CdpResponseHelper.formatBoolean(site.isPublished()));
		siteMap.put("created", CdpResponseHelper.dateTimeDisplayInUserZone(site.getCreatedTime().getTime()));
		siteMap.put("type", "course".equalsIgnoreCase(site.getType()) ? "Course" : "Project");
		// siteMap.put("owner", site.getCreatedBy().getSortName());
		siteMap.put("term", site.getTermDescription());
		siteMap.put("termId", site.getTermId());

		// Note: copied from SiteAction.java
		if (site.getProperties().getProperty("pub-date") != null)
		{
			try
			{
				// Note: originally, the date was stored in properties as input format, default time zone, rather than as a Time property -ggolden
				// If we fix this, we read the value with Time pubTime = siteProperties.getTimeProperty(PROP_SITE_PUB_DATE);
				String pubValue = site.getProperties().getProperty("pub-date");
				Date pubDate = DateHelper.parseDateFromDefault(pubValue);
				siteMap.put("publishOn", CdpResponseHelper.dateTimeDisplayInUserZone(pubDate.getTime()));

				// if this is in the future
				if (pubDate.after(new Date()))
				{
					siteMap.put("willPublish", CdpResponseHelper.formatBoolean(true));
				}
			}
			catch (ParseException e)
			{
			}
		}
		if (site.getProperties().getProperty("unpub-date") != null)
		{
			try
			{
				// Note: originally, the date was stored in properties as input format, default time zone, rather than as a Time property -ggolden
				// If we fix this, we read the value with Time pubTime = siteProperties.getTimeProperty(PROP_SITE_UNPUB_DATE);
				String unpubValue = site.getProperties().getProperty("unpub-date");
				Date unpubDate = DateHelper.parseDateFromDefault(unpubValue);
				siteMap.put("unpublishOn", CdpResponseHelper.dateTimeDisplayInUserZone(unpubDate.getTime()));
			}
			catch (ParseException e)
			{
			}
		}

		// instructor information
		List<Map<String, String>> instructors = new ArrayList<Map<String, String>>();
		siteMap.put("instructors", instructors);

		Set<String> instructorUserIds = (Set<String>) site.getUsersHasRole("Instructor");
		for (String userId : instructorUserIds)
		{
			try
			{
				User user = userDirectoryService().getUser(userId);

				Map<String, String> userMap = new HashMap<String, String>();
				instructors.add(userMap);

				userMap.put("name", user.getDisplayName());
				userMap.put("iid", user.getDisplayId());
				userMap.put("email", user.getEmail());
			}
			catch (UserNotDefinedException e)
			{
			}
		}

		siteMap.put("visitUnpublished", CdpResponseHelper.formatBoolean(Boolean.TRUE));
		siteMap.put("instructorPrivileges", CdpResponseHelper.formatBoolean(Boolean.TRUE));
	}

	/**
	 * @return The ArchiveService, via the component manager.
	 */
	private ArchivesService archivesService()
	{
		return (ArchivesService) ComponentManager.get(ArchivesService.class);
	}

	/**
	 * @return The AuthenticationManager, via the component manager.
	 */
	private PreferencesService preferencesService()
	{
		return (PreferencesService) ComponentManager.get(PreferencesService.class);
	}

	/**
	 * @return The PubDatesService, via the component manager.
	 */
	private PubDatesService pubDatesService()
	{
		return (PubDatesService) ComponentManager.get(PubDatesService.class);
	}

	/**
	 * @return The SiteService, via the component manager.
	 */
	private SiteService siteService()
	{
		return (SiteService) ComponentManager.get(SiteService.class);
	}

	/**
	 * @return The UserDirectoryService, via the component manager.
	 */
	private UserDirectoryService userDirectoryService()
	{
		return (UserDirectoryService) ComponentManager.get(UserDirectoryService.class);
	}
}
