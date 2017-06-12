tool_obj =
{
	title: "MY SITES",
	showReset: true,

	currentMode: 0,
	allSitesMode: false,
	siteId : null,
	sort: "",
	search: "",
	page: 1,
	first: 0,
	last: 0,
	count: 0,
	pageSize: 200,

	modes:
	[
		{
			title: "My Sites",
			elementId: "mysites_all",
			element: null,
			toolActionsElementId: "mysites_actions",
			toolItemTableElementId: "mysites_item_table",
			actions:
			[
				{title: "Site Setup", icon: "cog_edit.png", click: function(){tool_obj.configure(tool_obj);return false;}, select1Required: "selectSite"},
				{title: "Roster", icon: "group.png", click: function(){tool_obj.roster(tool_obj);return false;}, select1Required: "selectSite"},
				{title: "Publish", icon: "publish.png", click: function(){tool_obj.publish();return false;}, selectRequired: "selectSite"},
				{title: "Unpublish", icon: "publish_rmv.png", click: function(){tool_obj.unpublish();return false;}, selectRequired: "selectSite"}
			],
			actionsAllSitesAdmin:
			[
				{title: "Site Setup", icon: "cog_edit.png", click: function(){tool_obj.configure(tool_obj);return false;}, select1Required: "selectSite"},
				{title: "Roster", icon: "group.png", click: function(){tool_obj.roster(tool_obj);return false;}, select1Required: "selectSite"},
				{title: "Publish", icon: "publish.png", click: function(){tool_obj.publish();return false;}, selectRequired: "selectSite"},
				{title: "Unpublish", icon: "publish_rmv.png", click: function(){tool_obj.unpublish();return false;}, selectRequired: "selectSite"},
			 	{title: "Purge", icon: "delete.png", click: function(){tool_obj.purge(tool_obj);return false;}, selectRequired: "selectSite"}
			],
			actionsAllSitesHelpdesk:
			[
				{title: "Roster", icon: "group.png", click: function(){tool_obj.roster(tool_obj);return false;}, select1Required: "selectSite"}
			],
			headers:
			[
				{title: null, type: "checkbox", sort: false, checkboxId: "selectSite"},
				{title: null, type: "center", sort: false},
				{title: "Title", type: null, sort: true},
				{title: "Status", type: null, sort: true},
				{title: "Type", type: "padLeft", sort: true},
				{title: "Term", type: "padLeft", sort: true},
				{title: "Open Date", type: "padLeft", sort: true},
				{title: "Close Date", type: "padLeft", sort: true},
				{title: "Creation Date", type: "padLeft", sort: true}
			],
			headersAllSites:
			[
				{title: null, type: "checkbox", sort: false, checkboxId: "selectSite"},
				{title: "Title", type: null, sort: true},
				{title: "", type: null, sort: true},
				{title: "Term", type: "padLeft", sort: true},
				{title: "Creation Date", type: "padLeft", sort: true},
				{title: "Instructor", type: "padLeft", sort: true}
			],			
			start: function(obj, mode)
			{
				$("#mysites_favorites_order_link").removeClass("e3_offstage").unbind("click").click(function(){obj.openReorderDialog(obj);return false;});
				obj.doReset(obj);
			}
		}
	],

	start: function(obj, data)
	{
		if ((data !== undefined) && (data.toolMode !== undefined) && ($.isArray(data.toolMode)))
		{
			data.siteId = data.toolMode[4];
			obj.sort = data.toolMode[1];
			obj.search = data.toolMode[2];
			obj.page = data.toolMode[3];
			data.toolMode = data.toolMode[0];
		}

		if ((data.siteId == "admin") || (data.siteId == "helpdesk"))
		{
			// turn into all sites
			obj.allSitesMode = true;
			obj.siteId = data.siteId;
			obj.title = "ALL SITES";
			if (obj.sort == "") obj.sort = "date";
			if (data.siteId == "admin")
			{
				obj.modes[0].actions = obj.modes[0].actionsAllSitesAdmin;
			}
			else
			{
				obj.modes[0].actions = obj.modes[0].actionsAllSitesHelpdesk;
			}
			obj.modes[0].headers = obj.modes[0].headersAllSites;
			$("#allsites_go").unbind('click').click(function(){obj.reload(obj);return false;});
			$("#allsites_sort_date").unbind('click').click(function(){obj.reload(obj);return true;});
			$("#allsites_sort_term").unbind('click').click(function(){obj.reload(obj);return true;});
			
			$("#allsites_firstPage").unbind('click').click(function(){obj.firstPage(obj); return false;});
			$("#allsites_prevPage").unbind('click').click(function(){obj.prevPage(obj); return false;});
			$("#allsites_nextPage").unbind('click').click(function(){obj.nextPage(obj); return false;});
			$("#allsites_lastPage").unbind('click').click(function(){obj.lastPage(obj); return false;});

			$('input:radio[name=allsites_sort][value="' + obj.sort + '"]').prop('checked', true);
			$("#allsites_search").val(obj.search);
			
			$("#allsites_search").unbind('keydown').keydown(function(event)
			{
				if (event.keyCode == 13)
				{
					obj.reload(obj);
					return false;
				}
				return true;
			});

			$("#allsites_selection").removeClass("e3_offstage");
		}
		else
		{
			obj.sort = "term";
		}

		setupAlert("mysites_alertSelect");
		setupAlert("mysites_alertSelect1");
		setTitle(obj.title);
		setupConfirm("mysites_confirmPublish", "Publish", function(){obj.doPublish(obj);});
		setupConfirm("mysites_confirmUnpublish", "Unpublish", function(){obj.doUnpublish(obj);});
		setupConfirm("mysites_confirmFavorite", "Add To Tabs", function(){obj.doToggleFavorite(obj);});
		setupConfirm("mysites_confirmUnfavorite", "Remove From Tabs", function(){obj.doToggleFavorite(obj);});
		setupConfirm("mysites_confirmPurge", "Purge", function(){obj.doPurge(obj);});
		populateToolModes(obj);

		if ((data !== undefined) && (data.toolMode !== undefined))
		{
			selectToolMode(data.toolMode, obj);
		}
		
		startHeartbeat();
	},

	stop: function(obj, save)
	{
		stopHeartbeat();
	},

	reset: function(obj)
	{
		if (obj.allSitesMode)
		{
			obj.search = "";
			obj.sort = "date";
			obj.page = 1;
			$('input:radio[name=allsites_sort][value="' + obj.sort + '"]').prop('checked', true);
			$("#allsites_search").val(obj.search);
		}

		obj.doReset(obj);
	},

	reload: function(obj)
	{
		if (obj.allSitesMode)
		{
			obj.page = 1;
		}
		
		obj.doReset(obj);
	},

	doReset: function(obj)
	{
		if (obj.allSitesMode)
		{
			obj.sort = $('input:radio[name=allsites_sort]:checked').val();
			obj.search = $.trim($("#allsites_search").val());
			var params = new Object();
			params.search = obj.search;
			params.sort = obj.sort;
			params.page = obj.page.toString();
			params.pageSize = obj.pageSize.toString();
			requestCdp("mysites_allSites", params, function(data)
			{
				// stash the sites in userSites (for configure and roster)
				userSites.sites = data.sites;
				userSites.sitesByTerm = null;
				userSites.updated = null;
				
				obj.page = parseInt(data.page);
				obj.first = parseInt(data.first);
				obj.last = parseInt(data.last);
				obj.count = parseInt(data.count);
				
				$("#allsites_firstPage").removeClass("e3_disabled");
				$("#allsites_prevPage").removeClass("e3_disabled");
				$("#allsites_nextPage").removeClass("e3_disabled");
				$("#allsites_lastPage").removeClass("e3_disabled");
				if (obj.first == 1)
				{
					$("#allsites_firstPage").addClass("e3_disabled");
					$("#allsites_prevPage").addClass("e3_disabled");
				}
				if (obj.last == obj.count)
				{
					$("#allsites_nextPage").addClass("e3_disabled");
					$("#allsites_lastPage").addClass("e3_disabled");
				}

				$("#allsites_page").empty().text(obj.page.toString());
				$("#allsites_first").empty().text(obj.first.toString());
				$("#allsites_last").empty().text(obj.last.toString());
				$("#allsites_count").empty().text(obj.count.toString());

				obj.populateAllSites(obj, data.sites);
				adjustForNewHeight();
			});			
		}
		else
		{
			userSites.load(true, function()
			{
				obj.populateAllSites(obj, userSites.byTerm());
				obj.loadArchivedSites(obj);
			});
		}
	},

	firstPage: function(obj)
	{
		if (obj.first > 1)
		{
			obj.page = 1;
			obj.doReset(obj);
		}
	},

	prevPage: function(obj)
	{
		if (obj.first > 1)
		{
			obj.page = obj.page-1;
			obj.doReset(obj);
		}
	},

	nextPage: function(obj)
	{
		if (obj.last < obj.count)
		{
			obj.page = obj.page+1;
			obj.doReset(obj);
		}
	},

	lastPage: function(obj)
	{
		var numPages = Math.floor((obj.count-1) / obj.pageSize) + 1;
		if (obj.last < obj.count)
		{
			obj.page = numPages;
			obj.doReset(obj);
		}
	},

	loadArchivedSites: function(obj)
	{
		var data = new Object();		
		requestCdp("mysites_archives", data, function(data)
		{
			obj.populateArchivedSites(obj, data.archives);
			adjustForNewHeight();
		});
	},

	goToSite: function(siteId)
	{
		selectSite(siteId);
		return false;
	},

	populateAllSites: function(obj, sites)
	{
		$("#mysites_item_table tbody").empty();
		$("#mysites_noSites").addClass("e3_offstage");
		$("#mysites_noSitesAll").addClass("e3_offstage");

		var any = false;
		var other = false;
		if (sites != null)
		{
			$.each(sites, function(index, value)
			{
				any = true;

				var td;
				var tr = $("<tr />");
				$("#mysites_item_table tbody").append(tr);
				
				// insert an in-table heading if we are at the start of a new term
				if ((obj.sort == "term") && (((index > 0) && (sites[index-1].term != value.term)) || (index == 0)))
				{
					createHeaderTd(tr, value.term);

					// start over
					other = false;
					
					// we need a new row!
					tr = $("<tr />");
					$("#mysites_item_table tbody").append(tr);					
				}

				// zebra stripe
				if (other) $(tr).addClass("e3_table_stripe");
				other = !other;

				$(tr).addClass("mysitesData");

				// select box - if user has maintenance permissions
				if (value.instructorPrivileges == 1)
				{
					createSelectCheckboxTd(obj, tr, "selectSite", value.siteId);
				}
				else
				{
					createTextTd(tr, "");
				}

				// togglable icon for favorite
				if (!obj.allSitesMode)
				{
					createToggleIconTdIcons(tr, (value.visible == 1), "tab_gold.png", "tab_gray.png", "Remove Site From Your Tabs...", "Add Site To Your Tabs...",
							function(state){return obj.toggleFavorite(obj, value);});
				}

				// title
				if ((value.published == 1) || (value.visitUnpublished == 1))
				{
					if (obj.allSitesMode)
					{
						if (obj.siteId == "admin")
						{
							td = createHotTd(tr, value.title, function(){return openSite(value.siteId);});
						}
						else
						{
							td = createTextTd(tr, value.title);
						}
					}
					else
					{
						td = createHotTd(tr, value.title, function(){return obj.goToSite(value.siteId);});
					}
				}
				else
				{
					td = createTextTd(tr, value.title);
				}
				$(td).addClass("e3_nowrap");

				// publication status
				if (value.published == 1)
				{
					td = createIconTextTd(tr, "publish.png", (obj.allSitesMode ? "" : "Open"), "");
				}
				else
				{
					if (value.willPublish == 1)
					{
						td = createIconTextTd(tr, "calendar.png",  (obj.allSitesMode ? "" : "Will Open"), "");
					}
					else
					{
						td = createIconTextTd(tr, "closed.gif",  (obj.allSitesMode ? "" : "Closed"), "");
					}
				}
				$(td).addClass("e3_nowrap");

				// type
				if (!obj.allSitesMode)
				{
					td = createTextTd(tr, value.type);
					$(td).addClass("padLeft");
				}
				
				// term
				td = createTextTd(tr, value.term);
				$(td).addClass("e3_nowrap");
				$(td).addClass("padLeft");

				if (!obj.allSitesMode)
				{
					// published on
					td = createTextTd(tr, value.publishOn);
					$(td).addClass("e3_nowrap");
					$(td).addClass("padLeft");
					
					// unpublishedOn
					td = createTextTd(tr, value.unpublishOn);
					$(td).addClass("e3_nowrap");
					$(td).addClass("padLeft");
				}

				// creation date
				td = createTextTd(tr, value.created);
				$(td).addClass("e3_nowrap");
				$(td).addClass("padLeft");

				// instructor
				if (obj.allSitesMode)
				{
					var html = "";
					$.each(value.instructors, function(i, user)
					{
						html += "<a title='Email User' href='mailto:" + user.email + "'>" + user.name + " (" + user.iid + ")</a> ";
					});

					td = createHtmlTd(tr, html);
					$(td).addClass("padLeft");
				}
			});
		}
		
		if (!any)
		{
			if (obj.allSitesMode)
			{
				$("#mysites_noSitesAll").removeClass("e3_offstage");
			}
			else
			{
				$("#mysites_noSites").removeClass("e3_offstage");
			}
		}

		updateSelectStatus(obj, "selectSite");
	},

	populateArchivedSites: function(obj, sites)
	{
		$("#mysites_archives_table tbody").empty();
		$("#mysites_noArchives").addClass("e3_offstage");

		var any = false;
		if (sites != null)
		{
			$.each(sites, function(index, value)
			{
				any = true;

				var tr = $("<tr />");
				$("#mysites_archives_table tbody").append(tr);
				
				// insert an in-table heading if we are at the start of a new term
				if (((index > 0) && (sites[index-1].term != value.term)) || (index == 0))
				{
					createHeaderTd(tr, value.term);
					
					// we need a new row!
					tr = $("<tr />");
					$("#mysites_archives_table tbody").append(tr);					
				}

				// spacers
				createTextTd(tr, "");

				// title
				createTextTd(tr, value.title);
			});
		}

		if (!any)
		{
			$("#mysites_noArchives").removeClass("e3_offstage");
		}
	},

	favoriteEdit: null,

	toggleFavorite: function(obj, site)
	{
		// remember the site
		obj.favoriteEdit = site;

		// open the proper confirm
		if (site.visible == 1)
		{
			$("#mysites_confirmUnfavorite").dialog('open');
		}
		else
		{
			$("#mysites_confirmFavorite").dialog('open');
		}

		return false;
	},
	
	doToggleFavorite: function(obj)
	{
		// save
		var data = new Object();
		data.siteId = obj.favoriteEdit.siteId;
		data.favorite = (obj.favoriteEdit.visible == 1) ? "0" : "1";

		obj.favoriteEdit = null;

		requestCdp("mysites_setFavoriteSite", data, function(data)
		{
			resetPortal();
		});
	},

	configure: function(obj)
	{
		if (oneOidsSelected("selectSite"))
		{
			// get ids selected
			var data = new Object();
			data.siteIds = collectAllOidsArray("selectSite");
			data.siteId = collectSelectedOidsArray("selectSite")[0];
			data.returnTo = new Object();
			data.returnTo.toolId = "/mysites/mysites";
			data.returnTo.toolMode = 0;
			if (obj.allSitesMode)
			{
				data.returnTo.toolMode = [0, $('input:radio[name=allsites_sort]:checked').val(), $.trim($("#allsites_search").val()), obj.page, obj.siteId];
			}

			// if any selected
			if (data.siteIds.length > 0)
			{
				// switch to configure
				selectStandAloneTool("/configure/configure", data);
			}
		}
		
		else
		{
			// instruct
			$("#mysites_alertSelect1").dialog("open");
		}
	},
	
	roster: function(obj)
	{
		if (oneOidsSelected("selectSite"))
		{
			// get ids selected
			var data = new Object();
			data.siteIds = collectAllOidsArray("selectSite");
			data.siteId = collectSelectedOidsArray("selectSite")[0];
			data.returnTo = new Object();
			data.returnTo.toolId = "/mysites/mysites";
			data.returnTo.toolMode = 0;
			if (obj.allSitesMode)
			{
				data.returnTo.toolMode = [0, $('input:radio[name=allsites_sort]:checked').val(), $.trim($("#allsites_search").val()), obj.page, obj.siteId];
			}

			// if any selected
			if (data.siteIds.length > 0)
			{
				// switch to roster
				selectStandAloneTool("/siteroster/siteroster", data);
			}
		}
		
		else
		{
			// instruct
			$("#mysites_alertSelect1").dialog("open");
		}
	},

	publish: function()
	{
		if (anyOidsSelected("selectSite"))
		{
			// confirm
			$("#mysites_confirmPublish").dialog('open');
		}
		
		else
		{
			// instruct
			$("#mysites_alertSelect").dialog("open");
		}
	},

	doPublish: function(obj)
	{
		// get ids selected
		var data = new Object();
		data.siteIds = collectSelectedOids("selectSite");
		
		// if any selected
		if (data.siteIds.length > 0)
		{
			requestCdp("mysites_publishSites", data, function(data)
			{
				// reload the sites
				obj.doReset(obj);
			});
		}
	},

	unpublish: function()
	{
		if (anyOidsSelected("selectSite"))
		{
			// confirm
			$("#mysites_confirmUnpublish").dialog('open');
		}
		
		else
		{
			// instruct
			$("#mysites_alertSelect").dialog("open");
		}
	},

	doUnpublish: function(obj)
	{
		// get ids selected
		var data = new Object();
		data.siteIds = collectSelectedOids("selectSite");
		
		// if any selected
		if (data.siteIds.length > 0)
		{
			requestCdp("mysites_unpublishSites", data, function(data)
			{
				// reload the sites
				obj.doReset(obj);
			});
		}
	},

	purge: function(obj)
	{
		if (anyOidsSelected("selectSite"))
		{
			// confirm
			$("#mysites_confirmPurge").dialog('open');
		}
		
		else
		{
			// instruct
			$("#mysites_alertSelect").dialog("open");
		}
	},

	doPurge: function(obj)
	{
		var data = new Object();
		data.siteIds = collectSelectedOids("selectSite");
		requestCdp("mysites_purge", data, function(data)
		{
			// reload the sites
			obj.doReset(obj);
		});
	}
};

completeToolLoad();
