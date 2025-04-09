import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.olimp.data.repository.UserRepository
import com.example.olimp.databinding.FragmentFriendRequestsBinding
import com.example.olimp.network.FriendRequest
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.ui.friends.FriendsAdapter
import kotlinx.coroutines.launch

class FriendRequestsFragment : Fragment() {

    private var _binding: FragmentFriendRequestsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FriendsAdapter
    private lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendRequestsBinding.inflate(inflater, container, false)
        userRepository = UserRepository(RetrofitInstance.getApi(requireContext()))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FriendsAdapter(
            onAction1Click = { friendship -> acceptFriend(friendship.id) },
            onAction2Click = { friendship -> removeFriend(friendship.id) },
            isRequests = true
        )
        binding.rvRequests.layoutManager = LinearLayoutManager(context)
        binding.rvRequests.adapter = adapter

        loadRequests()
    }

    private fun loadRequests() {
        lifecycleScope.launch {
            try {
                val response = userRepository.getFriendRequests()
                if (response.isSuccessful) {
                    val requests = response.body() ?: emptyList()
                    adapter.submitList(requests)
                    binding.tvEmpty.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    private fun acceptFriend(friendshipId: Int) {
        lifecycleScope.launch {
            try {
                userRepository.acceptFriend(friendshipId)
                loadRequests()
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    private fun removeFriend(friendshipId: Int) {
        lifecycleScope.launch {
            try {
                userRepository.removeFriend(friendshipId)
                loadRequests()
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
