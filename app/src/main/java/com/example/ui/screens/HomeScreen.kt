package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MovieSubject
import com.example.ui.components.DashboardShimmerScreen
import com.example.ui.components.MovieGridItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MovieViewModel
import com.example.util.ImageOptimizer

import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Tune

// Top tab data
private data class HomeTab(val id: String, val label: String, val showLiveDot: Boolean = false)
private val homeTabs = listOf(
    HomeTab("live", "LIVE", showLiveDot = true),
    HomeTab("trending", "Trending"),
    HomeTab("tvseries", "TV/Series"),
    HomeTab("movie", "Movie"),
    HomeTab("kids", "Kids"),
)

@Composable
fun HomeScreen(
    viewModel: MovieViewModel,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToLiveTv: () -> Unit = {}
) {
    val trending by viewModel.trendingMovies.collectAsState()
    val categoryMovies by viewModel.categoryMovies.collectAsState()
    val categorySections by viewModel.categorySections.collectAsState()

    var selectedHomeTab by remember { mutableStateOf("trending") }

    val validSections = remember(categorySections) {
        categorySections.filter { sec ->
            sec.items.isNotEmpty() &&
            !sec.type.contains("BANNER", ignoreCase = true) &&
            !sec.title.contains("Banner", ignoreCase = true) &&
            !sec.title.equals("Categories", ignoreCase = true) &&
            !sec.title.contains("Star", ignoreCase = true) &&
            !sec.title.contains("Actor", ignoreCase = true) &&
            !sec.title.contains("Short", ignoreCase = true) &&
            !sec.title.contains("Join Us", ignoreCase = true) &&
            !sec.title.contains("Watch on TV", ignoreCase = true) &&
            !sec.title.contains("Get Free Premium", ignoreCase = true) &&
            !sec.type.contains("SINGLE_IMAGE", ignoreCase = true) &&
            !sec.type.contains("FILTER", ignoreCase = true) &&
            !sec.type.contains("ACTOR", ignoreCase = true) &&
            !sec.type.contains("STAR", ignoreCase = true)
        }
    }

    val bannerMovies = remember(categorySections, categoryMovies, validSections) {
        val bannerSec = categorySections.firstOrNull { it.type.contains("BANNER", ignoreCase = true) || it.title.contains("Banner", ignoreCase = true) }
        if (bannerSec != null && bannerSec.items.isNotEmpty()) {
            bannerSec.items.take(5)
        } else if (categoryMovies.isNotEmpty()) {
            categoryMovies.take(5)
        } else if (validSections.isNotEmpty()) {
            validSections.first().items.take(5)
        } else {
            emptyList()
        }
    }

    if (viewModel.isDashboardLoading) {
        DashboardShimmerScreen()
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepNavy)
        ) {
            // ── TOP BAR ──────────────────────────────────────────────
            item {
                HomeTopBar(onSearchClick = onNavigateToSearch)
            }

            // ── CATEGORY TABS ─────────────────────────────────────────
            item {
                HomeCategoryTabs(
                    selectedTab = selectedHomeTab,
                    onTabSelected = { selectedHomeTab = it },
                    onLiveClick = onNavigateToLiveTv
                )
            }

            // ── HERO BANNER ───────────────────────────────────────────
            if (bannerMovies.isNotEmpty()) {
                item {
                    HeroBannerSection(bannerMovies = bannerMovies, viewModel = viewModel)
                }
            }

            // ── CATEGORIES SECTION ────────────────────────────────────
            item {
                val categories by viewModel.activeCategories.collectAsState()
                LaunchedEffect(categories) {
                    if (categories.isNotEmpty() && categories.none { it.first == viewModel.selectedCategoryTab }) {
                        viewModel.loadCategoryList(categories.first().first)
                    }
                }
                if (categories.isNotEmpty()) {
                    SectionWithChips(
                        title = "Categories",
                        categories = categories,
                        selectedCategory = viewModel.selectedCategoryTab,
                        onCategorySelect = { viewModel.loadCategoryList(it) }
                    )
                }
            }

            // ── THEMATIC SECTIONS (Top picks, Trending, etc.) ─────────
            if (validSections.isNotEmpty()) {
                validSections.forEachIndexed { index, section ->
                    item {
                        SectionRow(
                            title = if (index == 0) "Top picks" else section.title,
                            movies = section.items,
                            onMovieClick = { viewModel.selectSubject(it) },
                            onSeeAll = {}
                        )
                    }
                }
            } else if (categoryMovies.isNotEmpty()) {
                item {
                    SectionRow(
                        title = "Top picks",
                        movies = categoryMovies,
                        onMovieClick = { viewModel.selectSubject(it) },
                        onSeeAll = {}
                    )
                }
            }

            // ── TRENDING AS LIVE & UPCOMING ───────────────────────────
            if (trending.isNotEmpty()) {
                item {
                    SectionRow(
                        title = "Live and upcoming",
                        movies = trending,
                        onMovieClick = { viewModel.selectSubject(it) },
                        onSeeAll = {},
                        showLiveBadge = true
                    )
                }
            }

            // Bottom spacing for nav bar
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun HomeTopBar(onSearchClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepNavy)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon placeholder (teal circle)
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(NeonTeal, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.LiveTv,
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Search bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .background(CardSurface, RoundedCornerShape(10.dp))
                .clickable { onSearchClick() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search movies / series",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Notification bell
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = "Notifications",
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun HomeCategoryTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onLiveClick: () -> Unit = {}
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepNavy)
            .padding(bottom = 6.dp)
    ) {
        items(homeTabs) { tab ->
            val isSelected = selectedTab == tab.id
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    if (tab.id == "live") {
                        onLiveClick()
                    } else {
                        onTabSelected(tab.id)
                    }
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tab.id == "live") {
                        Icon(
                            imageVector = Icons.Filled.LiveTv,
                            contentDescription = "Live",
                            tint = if (isSelected) NeonTeal else Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    } else if (tab.showLiveDot) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(HotRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = tab.label,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = if (isSelected) 14.sp else 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(if (isSelected) 28.dp else 0.dp)
                        .height(3.dp)
                        .background(if (isSelected) NeonTeal else Color.Transparent, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun HeroBannerSection(bannerMovies: List<MovieSubject>, viewModel: MovieViewModel) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { bannerMovies.size })

    LaunchedEffect(pagerState, bannerMovies) {
        while (bannerMovies.isNotEmpty()) {
            kotlinx.coroutines.delay(4500)
            val nextPage = (pagerState.currentPage + 1) % bannerMovies.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column {
        // Large hero image pager
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val heroMovie = bannerMovies[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { viewModel.selectSubject(heroMovie) }
                ) {
                    val optimalHeroUrl = remember(heroMovie.poster) {
                        ImageOptimizer.getOptimalUrl(heroMovie.poster, 720)
                    }
                    val heroAmbient = remember(heroMovie.averageHue) {
                        ImageOptimizer.parseAverageHue(heroMovie.averageHue)
                    }
                    val heroPlaceholder = remember(heroMovie.blurHash, heroAmbient) {
                        ImageOptimizer.getBlurHashPainter(heroMovie.blurHash, 30, 20)
                            ?: androidx.compose.ui.graphics.painter.ColorPainter(heroAmbient)
                    }
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(optimalHeroUrl)
                            .crossfade(false)
                            .build(),
                        contentDescription = heroMovie.name,
                        placeholder = heroPlaceholder,
                        error = heroPlaceholder,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Bottom gradient fade
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        DeepNavy.copy(alpha = 0.3f),
                                        DeepNavy
                                    )
                                )
                            )
                    )
                }
            }

            // Pager dots indicator
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(bannerMovies.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 18.dp else 6.dp, 6.dp)
                            .background(
                                if (pagerState.currentPage == index) NeonTeal else TextSecondary.copy(alpha = 0.5f),
                                RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
        }

        // Two mini cards below the banner
        val currentBannerIdx = pagerState.currentPage
        val card1 = bannerMovies.getOrNull(currentBannerIdx)
        val card2 = bannerMovies.getOrNull((currentBannerIdx + 1) % bannerMovies.size)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepNavy)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOfNotNull(card1, card2).forEachIndexed { idx, movie ->
                BannerMiniCard(
                    movie = movie,
                    onClick = { viewModel.selectSubject(movie) },
                    showPlayButton = (idx == 0),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BannerMiniCard(
    movie: MovieSubject,
    onClick: () -> Unit,
    showPlayButton: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(CardSurface, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumbUrl = remember(movie.poster) { ImageOptimizer.getOptimalUrl(movie.poster, 120) }
        val thumbColor = remember(movie.averageHue) { ImageOptimizer.parseAverageHue(movie.averageHue) }
        AsyncImage(
            model = coil.request.ImageRequest.Builder(LocalContext.current)
                .data(thumbUrl)
                .crossfade(false)
                .build(),
            contentDescription = movie.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp, 56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(thumbColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = movie.name,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = "Year",
                    tint = TextSecondary,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${movie.year ?: "2024"} ${movie.genres.firstOrNull() ?: ""}",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (showPlayButton) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(NeonTeal, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionWithChips(
    title: String,
    categories: List<Pair<Int, String>>,
    selectedCategory: Int,
    onCategorySelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
    ) {
        SectionHeader(title = title, onSeeAll = {})
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "All" chip
            item {
                val isAllSelected = selectedCategory == (categories.firstOrNull()?.first ?: 0)
                Box(
                    modifier = Modifier
                        .background(
                            if (isAllSelected) NeonTeal else ChipBg,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { categories.firstOrNull()?.let { onCategorySelect(it.first) } }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "All",
                        color = if (isAllSelected) Color.White else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Filter icon chip
            item {
                Box(
                    modifier = Modifier
                        .background(ChipBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Filter",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Category Chips (Hollywood, Nollywood, etc.)
            items(categories.drop(1)) { cat ->
                val isSelected = selectedCategory == cat.first
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) NeonTeal else CardSurface,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onCategorySelect(cat.first) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.second,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionRow(
    title: String,
    movies: List<MovieSubject>,
    onMovieClick: (MovieSubject) -> Unit,
    onSeeAll: () -> Unit,
    showLiveBadge: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
    ) {
        SectionHeader(title = title, onSeeAll = onSeeAll, showLiveBadge = showLiveBadge)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                MovieGridItem(
                    movie = movie,
                    showRating = !showLiveBadge,
                    showLiveBadge = showLiveBadge
                ) {
                    onMovieClick(movie)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit, showLiveBadge: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showLiveBadge) {
                Box(
                    modifier = Modifier
                        .background(HotRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SectionHeaderColor
            )
        }
        Text(
            text = "All >",
            fontSize = 13.sp,
            color = NeonTeal,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onSeeAll() }
        )
    }
}
