/*
 * Copyright (c) 2025, Ethan Hubbartt <ehubbartt@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.catracker.util;

import com.catracker.model.CombatAchievement;
import lombok.extern.slf4j.Slf4j;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CompletionPercentageLoader
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/Combat_Achievements/All_tasks";
	private final Map<String, Double> completionPercentages = new HashMap<>();
	private boolean isLoaded = false;

	private OkHttpClient okHttpClient;

	public CompletionPercentageLoader(OkHttpClient okHttpClient)
	{
		this.okHttpClient = okHttpClient;
	}

	public void setOkHttpClient(OkHttpClient okHttpClient)
	{
		this.okHttpClient = okHttpClient;
	}

	public CompletableFuture<Void> loadCompletionPercentagesAsync()
	{
		return CompletableFuture.runAsync(() ->
		{
			try
			{
				loadCompletionPercentages();
				isLoaded = true;
				log.debug("Completion percentage loading completed successfully");
			}
			catch (Exception e)
			{
				log.error("Failed to load completion percentages", e);
			}
		});
	}

	private void loadCompletionPercentages() throws Exception
	{
		Request request = new Request.Builder()
			.url(WIKI_URL)
			.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
			.build();

		try (Response response = okHttpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				String unsuccessful = "Wiki request unsuccessful with status " + response.code();
				if (response.body() == null)
				{
					unsuccessful += " and body \n" + response.body().string();
				}
				log.error(unsuccessful);
				throw new Exception(unsuccessful);
			}

			if (response.body() == null)
			{
				log.error("Wiki response returned without body");
				throw new Exception("Wiki response returned without body");
			}

			String html = response.body().string();
			parseCompletionPercentages(html);
		}
		catch (IOException e)
		{
			log.error("Failed to fetch completion percentages from wiki", e);
			throw new Exception("Failed to fetch completion percentages from wiki", e);
		}
	}

	private void parseCompletionPercentages(String html)
	{
		Pattern rowPattern = Pattern.compile("<tr[^>]*data-ca-task-id=\"\\d+\"[^>]*>(.*?)</tr>", Pattern.DOTALL);
		Matcher rowMatcher = rowPattern.matcher(html);

		while (rowMatcher.find())
		{
			String row = rowMatcher.group(1);

			String[] cells = row.split("</td>");
			if (cells.length >= 6)
			{
				String taskName = extractTaskName(cells[1]);
				String percentageStr = extractPercentage(cells[5]);

				if (taskName != null && percentageStr != null)
				{
					try
					{
						double percentage = Double.parseDouble(percentageStr);
						completionPercentages.put(taskName.trim(), percentage);
					}
					catch (NumberFormatException e)
					{
						log.warn("Could not parse percentage for task: {}", taskName);
					}
				}
			}
		}

		log.debug("Loaded {} completion percentages", completionPercentages.size());
	}

	private String extractTaskName(String cell)
	{
		Pattern namePattern = Pattern.compile("<a[^>]*title=\"([^\"]+)\"[^>]*>([^<]+)</a>");
		Matcher nameMatcher = namePattern.matcher(cell);
		if (nameMatcher.find())
		{
			return nameMatcher.group(2).trim();
		}
		return null;
	}

	private String extractPercentage(String cell)
	{
		Pattern percentagePattern = Pattern.compile("([0-9.]+)%");
		Matcher percentageMatcher = percentagePattern.matcher(cell);
		if (percentageMatcher.find())
		{
			return percentageMatcher.group(1);
		}
		return null;
	}

	public void hydrateAchievements(List<CombatAchievement> achievements)
	{
		if (achievements == null)
		{
			return;
		}

		for (CombatAchievement achievement : achievements)
		{
			String name = achievement.getName();
			Double percentage = completionPercentages.get(name);

			if (percentage == null)
			{
				String normalizedName = normalizeTaskName(name);
				for (Map.Entry<String, Double> entry : completionPercentages.entrySet())
				{
					if (normalizeTaskName(entry.getKey()).equals(normalizedName))
					{
						percentage = entry.getValue();
						break;
					}
				}
			}

			achievement.setCompletionPercentage(percentage);
		}
	}

	private String normalizeTaskName(String name)
	{
		return name.toLowerCase()
			.replaceAll("[^a-z0-9\\s]", "")
			.replaceAll("\\s+", " ")
			.trim();
	}

	public boolean isDataLoaded()
	{
		return isLoaded;
	}

	public Double getCompletionPercentage(String taskName)
	{
		return completionPercentages.get(taskName);
	}
}