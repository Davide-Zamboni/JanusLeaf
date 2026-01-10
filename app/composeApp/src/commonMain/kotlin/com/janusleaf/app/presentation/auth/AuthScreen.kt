package com.janusleaf.app.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.janusleaf.app.presentation.components.AnimatedBackground
import com.janusleaf.app.presentation.components.GradientOrb
import com.janusleaf.app.presentation.components.JanusPrimaryButton
import com.janusleaf.app.presentation.components.JanusTextField
import com.janusleaf.app.presentation.components.JanusTextButton
import com.janusleaf.app.presentation.theme.DuskPurple
import com.janusleaf.app.presentation.theme.ErrorRed
import com.janusleaf.app.presentation.theme.SageGreen
import com.janusleaf.app.presentation.theme.SunriseGold
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main authentication screen with login/register toggle.
 * Features a beautiful animated background and glassmorphism-styled form.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = koinViewModel(),
    onAuthSuccess: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffects.collectLatest { effect ->
            when (effect) {
                is AuthSideEffect.NavigateToHome -> onAuthSuccess()
                is AuthSideEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground {
            // Decorative gradient orbs
            GradientOrb(
                modifier = Modifier
                    .size(400.dp)
                    .offset(x = (-100).dp, y = (-50).dp)
                    .alpha(0.6f),
                color = SageGreen,
                size = 300f
            )
            
            GradientOrb(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 80.dp, y = 200.dp)
                    .alpha(0.4f),
                color = DuskPurple,
                size = 200f
            )
            
            GradientOrb(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-50).dp, y = 100.dp)
                    .alpha(0.5f),
                color = SunriseGold,
                size = 150f
            )
            
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                
                // App branding
                AuthHeader(authMode = state.authMode)
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Auth form card with glassmorphism effect
                AuthFormCard(
                    state = state,
                    onEvent = viewModel::onEvent
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Toggle auth mode
                AuthModeToggle(
                    authMode = state.authMode,
                    onToggle = { viewModel.onEvent(AuthEvent.ToggleAuthMode) }
                )
                
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        
        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(12.dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    }
}

@Composable
private fun AuthHeader(authMode: AuthMode) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated leaf emoji with glow effect
        Text(
            text = "🍃",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // App name
        Text(
            text = "JanusLeaf",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Tagline with animated text
        AnimatedContent(
            targetState = authMode,
            transitionSpec = {
                fadeIn(tween(300)) + slideInHorizontally { it / 2 } togetherWith
                fadeOut(tween(300)) + slideOutHorizontally { -it / 2 }
            },
            label = "tagline"
        ) { mode ->
            Text(
                text = when (mode) {
                    AuthMode.LOGIN -> "Welcome back to your journey"
                    AuthMode.REGISTER -> "Begin your mindful journey"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AuthFormCard(
    state: AuthState,
    onEvent: (AuthEvent) -> Unit
) {
    val cardElevation by animateDpAsState(
        targetValue = if (state.isLoading) 2.dp else 8.dp,
        animationSpec = spring(),
        label = "elevation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
            .padding(24.dp)
    ) {
        Column {
            // Error message
            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = state.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Email field
            JanusTextField(
                value = state.email,
                onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
                label = "Email",
                leadingIcon = Icons.Default.Email,
                error = state.emailError,
                keyboardType = KeyboardType.Email,
                imeAction = if (state.authMode == AuthMode.REGISTER) ImeAction.Next else ImeAction.Next,
                enabled = !state.isLoading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Username field (only for register)
            AnimatedVisibility(
                visible = state.authMode == AuthMode.REGISTER,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    JanusTextField(
                        value = state.username,
                        onValueChange = { onEvent(AuthEvent.UsernameChanged(it)) },
                        label = "Username",
                        leadingIcon = Icons.Default.Person,
                        error = state.usernameError,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        enabled = !state.isLoading
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Password field
            JanusTextField(
                value = state.password,
                onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                trailingIcon = if (state.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                onTrailingIconClick = { onEvent(AuthEvent.TogglePasswordVisibility) },
                isPassword = true,
                isPasswordVisible = state.isPasswordVisible,
                error = state.passwordError,
                keyboardType = KeyboardType.Password,
                imeAction = if (state.authMode == AuthMode.REGISTER) ImeAction.Next else ImeAction.Done,
                onImeAction = { if (state.authMode == AuthMode.LOGIN) onEvent(AuthEvent.Submit) },
                enabled = !state.isLoading
            )
            
            // Confirm password field (only for register)
            AnimatedVisibility(
                visible = state.authMode == AuthMode.REGISTER,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    JanusTextField(
                        value = state.confirmPassword,
                        onValueChange = { onEvent(AuthEvent.ConfirmPasswordChanged(it)) },
                        label = "Confirm Password",
                        leadingIcon = Icons.Default.Lock,
                        trailingIcon = if (state.isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        onTrailingIconClick = { onEvent(AuthEvent.ToggleConfirmPasswordVisibility) },
                        isPassword = true,
                        isPasswordVisible = state.isConfirmPasswordVisible,
                        error = state.confirmPasswordError,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onImeAction = { onEvent(AuthEvent.Submit) },
                        enabled = !state.isLoading
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Submit button
            JanusPrimaryButton(
                text = when (state.authMode) {
                    AuthMode.LOGIN -> "Sign In"
                    AuthMode.REGISTER -> "Create Account"
                },
                onClick = { onEvent(AuthEvent.Submit) },
                enabled = state.isFormValid && !state.isLoading,
                isLoading = state.isLoading
            )
        }
    }
}

@Composable
private fun AuthModeToggle(
    authMode: AuthMode,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (authMode) {
                AuthMode.LOGIN -> "Don't have an account?"
                AuthMode.REGISTER -> "Already have an account?"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        
        JanusTextButton(
            text = when (authMode) {
                AuthMode.LOGIN -> "Sign Up"
                AuthMode.REGISTER -> "Sign In"
            },
            onClick = onToggle
        )
    }
}
